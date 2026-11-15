package com.rommelguerra.prototipomusichaptic;
import com.rommelguerra.prototipomusichaptic.haptics.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class MusicalControlTests {
    private static void check(boolean ok) { if(!ok) throw new AssertionError("Musical protocol regression"); }
    public static void main(String[] args) throws Exception {
        check(!HapticProtocol.configureMusicalModel(null));
        check(!HapticProtocol.configureMusicalModel(new byte[]{0x4d,1,3,1,0,0,0,0}));
        check(HapticProtocol.configureMusicalModel(new byte[]{0x4d,1,3,1,100,0,(byte)232,3}));
        List<byte[]> commands=new ArrayList<>();
        int seq=1000;
        int previousL=101,previousR=-1;
        for(int pc=0;pc<12;pc++) {
            int[] full=HapticProtocol.musicalChannels(pc,100);
            check(full[0]<previousL && full[1]>previousR);
            previousL=full[0]; previousR=full[1];
            for(int intensity:new int[]{25,60,100}) {
                int[] levels=HapticProtocol.musicalChannels(pc,intensity);
                int center=0;
                byte[] command=HapticProtocol.commandV3(seq++,levels[0],levels[1],center,HapticProtocol.NOTE_CENTI_HZ[pc],500);
                check(command.length==15 && command[1]==3);
                commands.add(command);
                byte[] ack=command.clone(); ack[0]=0x41; ack[2]=0; ack[14]=HapticProtocol.checksum(ack);
                HapticProtocol.Ack decoded=HapticProtocol.decodeAck(ack);
                check(decoded!=null && decoded.matches(command) && decoded.carrierCentiHz==HapticProtocol.NOTE_CENTI_HZ[pc]);
                for(int i=0;i<ack.length;i++) for(int bit=0;bit<8;bit++) {
                    ack[i]^=(byte)(1<<bit); check(HapticProtocol.decodeAck(ack)==null); ack[i]^=(byte)(1<<bit);
                }
            }
        }
        check(Arrays.equals(HapticProtocol.musicalChannels(0,100),new int[]{100,0}));
        check(Arrays.equals(HapticProtocol.musicalChannels(11,100),new int[]{0,100}));
        commands.add(HapticProtocol.commandV3(seq,0,0,0,0,0));
        HapticCommandQueue q=new HapticCommandQueue();
        q.offer(80,0,0,0,500,100,6215); q.offer(0,0,0,110);
        q.offer(0,80,0,0,500,120,55383);
        check(q.poll(130).ms==0);
        HapticCommandQueue.Entry next=q.poll(140);
        check(next.carrierCentiHz==55383 && next.ms==480 && next.left==0);
        q.offer(80,0,0,0,100,100,6215); check(q.poll(200)==null);
        if(args.length>0) try(OutputStream out=Files.newOutputStream(Paths.get(args[0]))) {
            for(byte[] command:commands) out.write(command);
        }
        if(args.length>1) {
            byte[] bytes=Files.readAllBytes(Paths.get(args[1])); check(bytes.length==commands.size()*15);
            for(int i=0;i<commands.size();i++) {
                HapticProtocol.Ack ack=HapticProtocol.decodeAck(Arrays.copyOfRange(bytes,i*15,(i+1)*15));
                check(ack!=null && ack.matches(commands.get(i)));
            }
        }
        check(HapticIntensity.percent("ALTA")==100 && HapticIntensity.percent("MEDIA")==60);
        check(HapticIntensity.percent("BAJA")==25 && HapticIntensity.percent(null)==100);
        check(!HapticIntensity.valid("150") && HapticIntensity.valid("ALTA"));
        for(int pc=0;pc<12;pc++) check(Arrays.equals(HapticProtocol.musicalChannels(pc,0),new int[]{0,0}));
        for(byte[] frame:commands) check(frame[7]==0 && frame[8]==0 && frame[9]==0);
        System.out.println("PASS: intensity presets, musical model, 37 dual v3 frames, ACK corruption, queue and STOP.");
    }
}
