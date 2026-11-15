package com.rommelguerra.prototipomusichaptic.haptics;

import java.util.UUID;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Same wire format as firmware/include/HapticProtocol.h; no Android dependencies. */
public final class HapticProtocol {
    public static final int OK=0, BAD_FRAME=1, BAD_VALUE=2, STALE_COMMAND=3, CENTER_UNAVAILABLE=4;
    private static final Pattern NOTE = Pattern.compile("^(DO|RE|MI|FA|SOL|LA|SI|[A-G])([#B]?)(?:-1|[0-9])?$");
    public static final UUID SERVICE = UUID.fromString("12345678-1234-1234-1234-1234567890ab");
    public static final UUID COMMAND = UUID.fromString("abcdefab-1234-5678-1234-abcdefabcdef");
    public static final UUID STATUS = UUID.fromString("abcdefac-1234-5678-1234-abcdefabcdef");
    public static final UUID CAPABILITIES = UUID.fromString("abcdefad-1234-5678-1234-abcdefabcdef");
    public static final UUID MUSICAL_CAPABILITIES = UUID.fromString("abcdefae-1234-5678-1234-abcdefabcdef");
    public static final UUID LEVEL_CAPABILITIES = UUID.fromString("abcdefaf-1234-5678-1234-abcdefabcdef");
    public static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final String[] NAMES = {"Do", "Do♯", "Re", "Re♯", "Mi", "Fa", "Fa♯", "Sol", "Sol♯", "La", "La♯", "Si"};
    private HapticProtocol() {}
    public static byte checksum(byte[] b) {
        byte x = 0; for (int i = 0; i < b.length-1; i++) x ^= b[i]; return x;
    }
    public static byte[] command(int sequence, int left, int right, int ms) {
        if (sequence < 0 || sequence > 65535 || left < 0 || left > 100 || right < 0 || right > 100
                || ms < 0 || ms > 3000 || ((left != 0 || right != 0) && ms == 0))
            throw new IllegalArgumentException("Comando fuera de rango");
        boolean stop = left == 0 && right == 0;
        if (stop) ms = 0;
        byte[] b = {0x48, 1, (byte)(stop ? 0 : 1), (byte)sequence, (byte)(sequence >> 8),
                (byte)left, (byte)right, (byte)ms, (byte)(ms >> 8), 0};
        b[9] = checksum(b); return b;
    }
    public static byte[] commandV2(int sequence,int left,int right,int center,int hz,int ms) {
        if(sequence<0 || sequence>65535 || left<0 || left>100 || right<0 || right>100
                || center<0 || center>100 || ms<0 || ms>3000
                || (center>0 ? hz<20 || hz>500 : hz!=0))
            throw new IllegalArgumentException("Comando central fuera de rango");
        boolean stop=left==0 && right==0 && center==0;
        if(!stop && ms==0) throw new IllegalArgumentException("Duración cero");
        if(stop) ms=0;
        byte[] b={0x48,2,(byte)(stop ? 0 : 1),(byte)sequence,(byte)(sequence>>8),
            (byte)left,(byte)right,(byte)center,(byte)hz,(byte)(hz>>8),(byte)ms,(byte)(ms>>8),0};
        b[12]=checksum(b); return b;
    }
    public static final int[] NOTE_CENTI_HZ = {6215,7582,9250,11285,13768,16797,20492,25000,30500,37210,45396,55383};
    private static double funnelExponent=1, funnelDistanceMax=10;
    public static boolean configureMusicalModel(byte[] value) {
        if(value==null || value.length!=8 || value[0]!=0x4d || value[1]!=1 || value[2]!=3 || value[3]!=1) return false;
        double r=((value[4]&255)|((value[5]&255)<<8))/100.0;
        double span=((value[6]&255)|((value[7]&255)<<8))/100.0;
        if(r<=0 || span<=0) return false;
        funnelExponent=r; funnelDistanceMax=span; return true;
    }
    public static int[] musicalChannels(int midi, int intensity) {
        if(intensity<0 || intensity>100) throw new IllegalArgumentException("Intensidad");
        double d=Math.floorMod(midi,12)*funnelDistanceMax/11.0;
        return new int[]{(int)Math.round(intensity*Math.pow(Math.log1p(funnelDistanceMax-d)/Math.log1p(funnelDistanceMax),funnelExponent)),
            (int)Math.round(intensity*Math.pow(Math.log1p(d)/Math.log1p(funnelDistanceMax),funnelExponent))};
    }
    public static byte[] commandV3(int sequence,int left,int right,int center,int carrierCentiHz,int ms) {
        boolean stop=left==0 && right==0 && center==0;
        if(!stop && (carrierCentiHz<6000 || carrierCentiHz>60000)) throw new IllegalArgumentException("Frecuencia");
        byte[] b=java.util.Arrays.copyOf(commandV2(sequence,left,right,center,center>0 ? 160 : 0,ms),15);
        b[1]=3;
        int hz=center>0 ? Math.round(carrierCentiHz/100.0f) : 0;
        b[8]=(byte)hz; b[9]=(byte)(hz>>8);
        if(stop) carrierCentiHz=0;
        b[12]=(byte)carrierCentiHz; b[13]=(byte)(carrierCentiHz>>8); b[14]=checksum(b);
        return b;
    }
    /** v4 keeps the spatial pattern separate from the user's level (applied after GAIN). */
    public static byte[] commandV4(int sequence,int left,int right,int carrierCentiHz,int intensity,int ms) {
        if(intensity<0 || intensity>100) throw new IllegalArgumentException("Intensidad global");
        boolean stop=(left==0 && right==0) || intensity==0;
        byte[] b=java.util.Arrays.copyOf(commandV3(sequence,stop?0:left,stop?0:right,0,
            stop?0:carrierCentiHz,stop?0:ms),16);
        b[1]=4; b[14]=(byte)(stop?0:intensity); b[15]=checksum(b); return b;
    }
    public static boolean supportsLevelControl(byte[] b) {
        return b!=null && b.length==4 && b[0]==0x49 && b[1]==1 && b[2]==4 && b[3]==1;
    }
    private static int frameSize(int version) { return version==4 ? 16 : version==3 ? 15 : version==2 ? 13 : 10; }
    public static final class Capabilities {
        public final int protocol;
        public final boolean centerAvailable;
        private Capabilities(byte[] b) { protocol=b[2]; centerAvailable=b[3]==1; }
    }
    public static Capabilities capabilities(byte[] b) {
        return b!=null && b.length==4 && b[0]==0x43 && b[1]==1 && b[2]==2 && (b[3]==0 || b[3]==1)
            ? new Capabilities(b) : null;
    }
    public static final class Ack {
        public final int result,sequence,left,right,center,hz,duration,version,carrierCentiHz,intensity;
        Ack(byte[] b) {
            version=b[1]; result=b[2]&255; sequence=(b[3]&255)|((b[4]&255)<<8);
            left=b[5]&255; right=b[6]&255;
            center=version>=2 ? b[7]&255 : 0;
            hz=version>=2 ? (b[8]&255)|((b[9]&255)<<8) : 0;
            carrierCentiHz=version>=3 ? (b[12]&255)|((b[13]&255)<<8) : 0;
            intensity=version==4 ? b[14]&255 : 100;
            int d=version>=2 ? 10 : 7;
            duration=(b[d]&255)|((b[d+1]&255)<<8);
        }
        public boolean matches(byte[] sent) {
            if(sent==null || sent.length!=frameSize(version) || sent[1]!=version) return false;
            Ack target=new Ack(sent);
            return sequence==target.sequence && left==target.left && right==target.right
                && center==target.center && hz==target.hz && duration==target.duration && carrierCentiHz==target.carrierCentiHz && intensity==target.intensity;
        }
    }
    public static Ack decodeAck(byte[] b) {
        if(b==null || b.length<2 || b[0]!=0x41 || (b[1]!=1 && b[1]!=2 && b[1]!=3 && b[1]!=4)
            || b.length!=frameSize(b[1]) || checksum(b)!=b[b.length-1]) return null;
        Ack a=new Ack(b);
        if(a.result>CENTER_UNAVAILABLE || a.left>100 || a.right>100 || a.center>100 || a.duration>3000 || a.intensity>100
            || (a.center>0 ? a.hz<20 || a.hz>(a.version>=3 ? 600 : 500) : a.hz!=0)) return null;
        if(a.carrierCentiHz!=0 && (a.carrierCentiHz<6000 || a.carrierCentiHz>60000)) return null;
        if(a.version>=3 && a.result==OK && (a.left!=0 || a.right!=0 || a.center!=0) && a.carrierCentiHz==0) return null;
        return a.result==OK || (a.left==0 && a.right==0 && a.center==0 && a.hz==0 && a.duration==0 && a.carrierCentiHz==0 && (a.version!=4 || a.intensity==0)) ? a : null;
    }
    public static int pitchClass(String name) {
        if(name==null) return -1;
        Matcher match=NOTE.matcher(name.trim().toUpperCase(Locale.ROOT).replace('\u266f','#').replace('\u266d','B'));
        if(!match.matches()) return -1;
        int pc;
        switch(match.group(1)) {
            case "DO": case "C": pc=0; break;
            case "RE": case "D": pc=2; break;
            case "MI": case "E": pc=4; break;
            case "FA": case "F": pc=5; break;
            case "SOL": case "G": pc=7; break;
            case "LA": case "A": pc=9; break;
            default: pc=11;
        }
        return Math.floorMod(pc+("#".equals(match.group(2)) ? 1 : "B".equals(match.group(2)) ? -1 : 0),12);
    }
    /** Known notes always share the mic/firmware map; custom tactons retain their position. */
    public static int[] channelsForNote(String name,double position,int intensity) {
        int pc=pitchClass(name);
        if(pc>=0) return channels(pc,intensity);
        if(intensity<0 || intensity>100 || !Double.isFinite(position))
            throw new IllegalArgumentException("Posición o intensidad inválida");
        double pos=Math.max(0,Math.min(10,position))/10;
        return new int[]{pos==1 ? 0 : (int)Math.round(intensity*Math.cos(pos*Math.PI/2)),
                pos==0 ? 0 : (int)Math.round(intensity*Math.sin(pos*Math.PI/2))};
    }
    public static int[] channels(int midi, int intensity) {
        if (intensity < 0 || intensity > 100) throw new IllegalArgumentException("Intensidad");
        int pitchClass = Math.floorMod(midi, 12);
        double angle = (pitchClass / 11.0) * Math.PI / 2;
        return new int[]{pitchClass == 11 ? 0 : (int)Math.round(intensity*Math.cos(angle)),
                pitchClass == 0 ? 0 : (int)Math.round(intensity*Math.sin(angle))};
    }
    public static String noteName(int midi) {
        return NAMES[Math.floorMod(midi,12)] + (Math.floorDiv(midi,12)-1);
    }
}
