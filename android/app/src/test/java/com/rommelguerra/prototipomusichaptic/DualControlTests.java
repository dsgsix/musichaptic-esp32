package com.rommelguerra.prototipomusichaptic;

import com.rommelguerra.prototipomusichaptic.haptics.*;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Regressions executable with a JDK, also invoked from Gradle's unit tests. */
public final class DualControlTests {
    private static int assertions;
    private static void check(boolean ok,String message) {
        assertions++; if(!ok) throw new AssertionError(message);
    }
    private static void queueDeadlines() {
        HapticCommandQueue q=new HapticCommandQueue();
        q.offer(80,0,30,100); check(q.poll(130)==null,"30 ms note expired exactly at its end");
        q.offer(80,0,100,200); check(q.poll(275).ms==25,"Queue time must reduce duration");
        q.offer(80,0,350,1000); check(q.poll(1250).ms==100,"250 ms latency boundary");
        q.offer(80,0,350,1000); check(q.poll(1251)==null,"Discard after latency cap");
        q.offer(80,0,500,0); q.offer(0,0,0,1); q.offer(0,90,300,2);
        check(q.poll(100).ms==0,"STOP has priority");
        HapticCommandQueue.Entry e=q.poll(101);
        check(e.left==0 && e.right==90 && e.ms==201,"Latest subsequent note survives STOP");
        check(q.poll(102)==null,"Queue drained");
        q.offer(0,0,123,10); check(q.poll(10000).ms==0,"STOP never expires and is canonical");
        q.offer(0,0,0,1); q.offer(80,0,30,2);
        check(q.poll(100).ms==0,"STOP delivered even if following note expired");
        check(q.poll(101)==null,"Expired play after STOP is removed");
        q.offer(90,0,100,0); q.offer(0,0,0,1); q.offer(0,80,100,2); q.offer(0,0,0,3);
        check(q.poll(4).ms==0 && q.poll(4)==null,"Second STOP removes following play");
        q.offer(90,0,100,0); q.clear(); check(q.poll(1)==null,"Disconnect clears queue");
    }
    private static void mapping() {
        check(Arrays.equals(HapticProtocol.channelsForNote("DO",10,80),new int[]{80,0}),"Do overrides stale right position");
        check(Arrays.equals(HapticProtocol.channelsForNote("SI4",0,80),new int[]{0,80}),"Si overrides stale left position");
        String[] aliases={"DO#","C#4","Db3","REb","do♯","re♭"};
        for(String name:aliases) check(HapticProtocol.pitchClass(name)==1,"Sharp/flat alias "+name);
        check(HapticProtocol.pitchClass("B-1")==11,"B octave -1");
        check(HapticProtocol.pitchClass("DO10")==-1,"Reject invalid octave");
        check(Arrays.equals(HapticProtocol.channelsForNote("tacton",10,80),new int[]{0,80}),"Custom position preserved");
        for(int pc=0;pc<12;pc++) for(int intensity=0;intensity<=100;intensity++) {
            int[] ch=HapticProtocol.channelsForNote(new String[]{"DO","DO#","RE","RE#","MI","FA","FA#","SOL","SOL#","LA","LA#","SI"}[pc],0,intensity);
            check(Arrays.equals(ch,HapticProtocol.channels(60+pc,intensity)),"Button and mic map match");
        }
    }
    private static void acknowledgements() {
        for(int code=0;code<=5;code++) {
            byte[] a={0x41,1,(byte)code,7,0,0,0,0,0,0}; a[9]=HapticProtocol.checksum(a);
            check((HapticProtocol.decodeAck(a)!=null)==(code<=4),"ACK result "+code);
            if(code==3) {
                a[5]=1; a[9]=HapticProtocol.checksum(a);
                check(HapticProtocol.decodeAck(a)==null,"Rejected ACK cannot report active output");
            }
        }
    }
    private static int[] at(List<HapticTimeline.Segment> segments,long time) {
        for(HapticTimeline.Segment s:segments) if(time>=s.startMs && time<s.endMs) return new int[]{s.left,s.right};
        return new int[]{0,0};
    }
    private static void overlap() {
        List<HapticTimeline.Note> notes=Arrays.asList(new HapticTimeline.Note(100,900,80,0),new HapticTimeline.Note(300,300,0,90));
        List<HapticTimeline.Segment> segments=HapticTimeline.mix(notes);
        check(Arrays.equals(at(segments,50),new int[]{0,0}),"Initial silence");
        check(Arrays.equals(at(segments,100),new int[]{80,0}),"Left starts");
        check(Arrays.equals(at(segments,300),new int[]{80,90}),"Chord retains both sides");
        check(Arrays.equals(at(segments,600),new int[]{80,0}),"Left resumes after shorter Si ends");
        check(Arrays.equals(at(segments,1000),new int[]{0,0}),"Final stop");
        check(HapticTimeline.mix(Collections.emptyList()).isEmpty(),"Empty track");
        Random random=new Random(2048);
        for(int trial=0;trial<100;trial++) {
            List<HapticTimeline.Note> input=new ArrayList<>();
            for(int n=0;n<40;n++) input.add(new HapticTimeline.Note(random.nextInt(500),1+random.nextInt(500),random.nextInt(101),random.nextInt(101)));
            List<HapticTimeline.Segment> mixed=HapticTimeline.mix(input);
            long end=0;
            for(HapticTimeline.Segment segment:mixed) {
                check(segment.startMs==end && segment.endMs>segment.startMs,"Continuous positive-length timeline");
                end=segment.endMs;
            }
            for(int t=0;t<=1000;t++) {
                int left=0,right=0;
                for(HapticTimeline.Note note:input) if(t>=note.startMs && t<note.startMs+note.durationMs) {
                    left=Math.max(left,note.left); right=Math.max(right,note.right);
                }
                check(Arrays.equals(at(mixed,t),new int[]{left,right}),"Overlap oracle t="+t);
            }
        }
    }
    private static byte[] vectors() {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream(); Random r=new Random(20260920);
        for(int i=0;i<1000;i++) {
            byte[] c=HapticProtocol.command(i==0 ? 65535 : i,r.nextInt(101),r.nextInt(101),1+r.nextInt(3000));
            bytes.write(c,0,c.length);
        }
        for(int i=0;i<2;i++) { byte[] c=HapticProtocol.command(i,0,0,0); bytes.write(c,0,c.length); }
        return bytes.toByteArray();
    }
    public static void main(String[] args) throws Exception {
        queueDeadlines(); mapping(); acknowledgements(); overlap();
        byte[] commands=vectors();
        if(args.length==1) Files.write(Path.of(args[0]),commands);
        if(args.length==2) {
            byte[] replies=Files.readAllBytes(Path.of(args[1]));
            check(replies.length==commands.length,"Firmware ACK count");
            for(int i=0;i<commands.length;i+=10) {
                HapticProtocol.Ack ack=HapticProtocol.decodeAck(Arrays.copyOfRange(replies,i,i+10));
                check(ack!=null && ack.result==0,"Decode firmware ACK");
                check(ack.sequence==((commands[i+3]&255)|((commands[i+4]&255)<<8)),"Sequence roundtrip");
                check(ack.left==(commands[i+5]&255) && ack.right==(commands[i+6]&255),"Channels roundtrip");
                check(ack.duration==((commands[i+7]&255)|((commands[i+8]&255)<<8)),"Duration roundtrip");
            }
        }
        System.out.println("PASS: "+assertions+" assertions; deadlines, mapping, ACK v3, MIDI overlaps, wire roundtrip when supplied.");
    }
}
