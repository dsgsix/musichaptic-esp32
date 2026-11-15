package com.rommelguerra.prototipomusichaptic.haptics;

/** Bounded-memory mono resampler, 2-pole anti-alias filter and overlapping YIN windows. */
public final class PcmPitchStream {
    public interface Listener { void onFrame(PitchDetector.Result result, double startSeconds); }
    private final Listener listener;
    private final PitchDetector detector = new PitchDetector();
    private final short[] frame = new short[PitchDetector.FRAME_SIZE];
    private final double step, b0,b1,b2,a1,a2;
    private double x1,x2,y1,y2,previous,nextPosition;
    private long sourceSamples,outputSamples;
    private int fill;
    public PcmPitchStream(int sampleRate, Listener listener) {
        if(sampleRate<8000 || sampleRate>192000) throw new IllegalArgumentException("Frecuencia de muestreo no admitida");
        this.listener=listener; step=sampleRate/(double)PitchDetector.SAMPLE_RATE;
        double w=2*Math.PI*2000/sampleRate, c=Math.cos(w), alpha=Math.sin(w)/Math.sqrt(2), norm=1+alpha;
        b0=(1-c)/2/norm; b1=(1-c)/norm; b2=b0; a1=-2*c/norm; a2=(1-alpha)/norm;
    }
    public double seconds() { return sourceSamples / (step * PitchDetector.SAMPLE_RATE); }
    public void accept(double sample) {
        if(!Double.isFinite(sample)) sample=0;
        double filtered=b0*sample+b1*x1+b2*x2-a1*y1-a2*y2;
        x2=x1; x1=sample; y2=y1; y1=filtered;
        long index=sourceSamples++;
        if(index==0) { previous=filtered; return; }
        while(nextPosition<=index) {
            double fraction=Math.max(0,Math.min(1,nextPosition-(index-1)));
            double v=previous+(filtered-previous)*fraction;
            frame[fill++]=(short)Math.max(-32768,Math.min(32767,Math.round(v*32767)));
            outputSamples++; nextPosition+=step;
            if(fill==frame.length) {
                listener.onFrame(detector.analyze(frame,-45),(outputSamples-frame.length)/(double)PitchDetector.SAMPLE_RATE);
                System.arraycopy(frame,frame.length/2,frame,0,frame.length/2);
                fill=frame.length/2;
            }
        }
        previous=filtered;
    }
}
