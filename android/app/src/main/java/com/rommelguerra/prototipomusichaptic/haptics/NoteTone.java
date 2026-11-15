package com.rommelguerra.prototipomusichaptic.haptics;

/** Audible C4..B4, independent from the Weber haptic table and motor position. */
public final class NoteTone {
    public static final int SAMPLE_RATE = 44100;
    private NoteTone() {}
    public static double frequency(int pitchClass) {
        if(pitchClass<0 || pitchClass>11) throw new IllegalArgumentException("Nota");
        return 440.0 * Math.pow(2.0,(pitchClass-9)/12.0);
    }
    public static short[] pcm(int pitchClass,int durationMs) {
        if(durationMs<1 || durationMs>30000) throw new IllegalArgumentException("Duración");
        double hz=frequency(pitchClass);
        int size=Math.max(1,(int)((long)SAMPLE_RATE*durationMs/1000));
        short[] samples=new short[size];
        int attack=Math.min(size/2,SAMPLE_RATE*8/1000);
        int release=Math.min(size/2,SAMPLE_RATE*15/1000);
        for(int i=0;i<size;i++) {
            double phase=2*Math.PI*hz*i/SAMPLE_RATE;
            double envelope=Math.min(1.0,Math.min(i/(double)Math.max(1,attack),
                (size-1-i)/(double)Math.max(1,release)));
            // Quiet upper partials improve audibility on small speakers; no clipping.
            double value=(Math.sin(phase)+0.25*Math.sin(2*phase)+0.12*Math.sin(3*phase))/1.37;
            samples[i]=(short)Math.round(0.65*32767*envelope*value);
        }
        return samples;
    }
}
