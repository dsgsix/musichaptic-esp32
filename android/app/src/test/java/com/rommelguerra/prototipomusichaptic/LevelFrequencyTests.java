package com.rommelguerra.prototipomusichaptic;
import com.rommelguerra.prototipomusichaptic.haptics.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;

public final class LevelFrequencyTests {
    private static void check(boolean condition) { if(!condition) throw new AssertionError("Level/frequency regression"); }
    public static void main(String[] args) throws Exception {
        check(HapticProtocol.supportsLevelControl(new byte[]{0x49,1,4,1}));
        check(!HapticProtocol.supportsLevelControl(new byte[]{0x49,1,3,1}));
        List<byte[]> frames=new ArrayList<>();
        for(int pc=0;pc<12;pc++) for(int intensity:new int[]{25,60,100}) {
            int[] base=HapticProtocol.musicalChannels(pc,100);
            byte[] frame=HapticProtocol.commandV4(frames.size(),base[0],base[1],
                HapticProtocol.NOTE_CENTI_HZ[pc],intensity,500);
            check((frame[5]&255)==base[0] && (frame[6]&255)==base[1]);
            check((frame[14]&255)==intensity);
            frames.add(frame);
            byte[] ack=frame.clone(); ack[0]=0x41; ack[2]=0; ack[15]=HapticProtocol.checksum(ack);
            check(HapticProtocol.decodeAck(ack).matches(frame));
            for(int i=0;i<ack.length;i++) for(int bit=0;bit<8;bit++) {
                ack[i]^=(byte)(1<<bit); check(HapticProtocol.decodeAck(ack)==null); ack[i]^=(byte)(1<<bit);
            }
        }
        frames.add(HapticProtocol.commandV4(36,0,0,0,100,0));
        byte[] mute=HapticProtocol.commandV4(37,100,0,6215,0,500);
        check(mute[2]==0 && mute[5]==0 && mute[14]==0); frames.add(mute);
        HapticCommandQueue q=new HapticCommandQueue();
        q.offer(100,0,0,0,500,100,6215,25);
        q.offer(100,0,0,0,500,120,6215,60);
        HapticCommandQueue.Entry e=q.poll(140);
        check(e.intensity==60 && e.ms==480 && e.carrierCentiHz==6215);
        q.offer(100,0,0,0,500,100,6215,25); q.offer(0,0,0,110);
        q.offer(0,100,0,0,500,120,55383,100);
        check(q.poll(130).ms==0); check(q.poll(130).intensity==100);
        // Adjacent equal-level notes must not merge when their frequencies differ.
        List<HapticTimeline.Segment> timeline=HapticTimeline.mix(Arrays.asList(
            new HapticTimeline.Note(0,500,50,50,6215),new HapticTimeline.Note(500,500,50,50,55383)));
        check(timeline.size()==2 && timeline.get(0).carrierCentiHz==6215 && timeline.get(1).carrierCentiHz==55383);
        // A newer note temporarily owns both sides; the held note resumes afterwards.
        timeline=HapticTimeline.mix(Arrays.asList(new HapticTimeline.Note(0,1000,100,0,6215),
            new HapticTimeline.Note(200,300,0,100,55383),new HapticTimeline.Note(1500,100,20,50,25000)));
        check(timeline.size()==5 && timeline.get(1).left==0 && timeline.get(1).carrierCentiHz==55383);
        check(timeline.get(2).carrierCentiHz==6215 && timeline.get(3).left==0 && timeline.get(3).carrierCentiHz==0);
        // Audible pitch is C4..B4, not the haptic 62..554 Hz table.
        check(Math.abs(NoteTone.frequency(0)-261.625565)<0.0001 && NoteTone.frequency(9)==440);
        for(int pc=0;pc<12;pc++) {
            short[] pcm=NoteTone.pcm(pc,1000);
            check(pcm.length==44100 && pcm[0]==0 && pcm[pcm.length-1]==0);
            double energy=0; int crossings=0;
            for(int i=1;i<pcm.length;i++) {
                check(Math.abs((int)pcm[i])<32767); energy+=(double)pcm[i]*pcm[i];
                if(pcm[i-1]<0 && pcm[i]>=0) crossings++;
            }
            check(Math.sqrt(energy/pcm.length)>7000 && Math.abs(crossings-NoteTone.frequency(pc))<2);
        }
        if(args.length>0) try(OutputStream out=Files.newOutputStream(Paths.get(args[0]))) { for(byte[] f:frames) out.write(f); }
        if(args.length>1) {
            byte[] acks=Files.readAllBytes(Paths.get(args[1])); check(acks.length==frames.size()*16);
            for(int i=0;i<frames.size();i++) {
                HapticProtocol.Ack ack=HapticProtocol.decodeAck(Arrays.copyOfRange(acks,i*16,(i+1)*16));
                check(ack!=null && ack.matches(frames.get(i)));
            }
        }
        System.out.println("PASS: v4 level control, 38 frames, corruption, live queue changes, song frequency/overlap/silence and 12 audible mono tones.");
    }
}
