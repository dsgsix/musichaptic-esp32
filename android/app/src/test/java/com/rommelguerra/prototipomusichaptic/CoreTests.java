package com.rommelguerra.prototipomusichaptic;

import com.rommelguerra.prototipomusichaptic.haptics.HapticProtocol;
import com.rommelguerra.prototipomusichaptic.haptics.PitchDetector;
import java.util.Random;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class CoreTests {
    private static int assertions;
    static void check(boolean condition,String message) {
        assertions++; if (!condition) throw new AssertionError(message);
    }
    static short[] tone(double hz,double fundamental,double harmonic,double noise,int seed) {
        short[] samples=new short[PitchDetector.FRAME_SIZE]; Random random=new Random(seed);
        for (int i=0;i<samples.length;i++) {
            double phase=2*Math.PI*hz*i/PitchDetector.SAMPLE_RATE;
            double value=fundamental*Math.sin(phase+.31)+harmonic*Math.sin(2*phase)+noise*random.nextGaussian();
            samples[i]=(short)Math.round(Math.max(-1,Math.min(1,value))*32767);
        }
        return samples;
    }
    public static void main(String[] args) throws Exception {
        PitchDetector detector=new PitchDetector();
        check(detector.analyze(new short[2048],-45).midi==-1,"Silence must not vibrate");
        short[] dc=new short[2048]; Arrays.fill(dc,(short)8000);
        check(detector.analyze(dc,-45).midi==-1,"DC must not vibrate");
        for (int midi=36;midi<=83;midi++) {
            double hz=440*Math.pow(2,(midi-69)/12.0);
            for (int seed=0;seed<3;seed++) {
                PitchDetector.Result r=detector.analyze(tone(hz,.20,.30,.004,seed),-45);
                check(r.midi==midi,"Harmonics/noise, MIDI "+midi+" -> "+r.midi+" at "+r.frequency);
                check(Math.abs(1200*Math.log(r.frequency/hz)/Math.log(2))<12,"Frequency error MIDI "+midi);
            }
            int[] ch=HapticProtocol.channels(midi,80);
            check(ch[0]>=0 && ch[0]<=80 && ch[1]>=0 && ch[1]<=80,"Channel bounds");
        }
        for (int octave=2;octave<=5;octave++) {
            int[] left=HapticProtocol.channels((octave+1)*12,80);
            int[] right=HapticProtocol.channels((octave+1)*12+11,80);
            check(Arrays.equals(left,new int[]{80,0}),"C left");
            check(Arrays.equals(right,new int[]{0,80}),"B right");
        }
        for (int seed=0;seed<20;seed++) check(detector.analyze(tone(440,0,0,.05,seed),-45).midi==-1,"Noise rejection");
        check(detector.analyze(tone(440,.0001,0,0,0),-45).midi==-1,"Below noise gate");
        byte[] c=HapticProtocol.command(0x1234,80,0,500);
        byte[] expected={(byte)0x48,1,1,0x34,0x12,80,0,(byte)0xf4,1,(byte)0xcb};
        check(Arrays.equals(c,expected),"Golden wire frame: "+Arrays.toString(c));
        byte[] a=c.clone(); a[0]=0x41; a[2]=0; a[9]=HapticProtocol.checksum(a);
        HapticProtocol.Ack ack=HapticProtocol.decodeAck(a);
        check(ack!=null && ack.left==80 && ack.right==0 && ack.sequence==0x1234 && ack.duration==500,"ACK parse");
        a[5]^=1; check(HapticProtocol.decodeAck(a)==null,"Reject corrupt ACK");
        check(HapticProtocol.decodeAck(new byte[2])==null,"Reject short ACK");
        byte[] stop=HapticProtocol.command(65535,0,0,999);
        check(stop[2]==0 && stop[7]==0 && stop[8]==0,"Canonical stop");
        boolean threw=false; try { HapticProtocol.command(1,101,0,400); } catch (IllegalArgumentException e) { threw=true; }
        check(threw,"Reject invalid intensity");
        if (args.length>0) Files.write(Path.of(args[0]),c);
        System.out.println("PASS: "+assertions+" assertions; DSP: 48 notes with stronger 2nd harmonic/noise; protocol and mapping.");
    }
}
