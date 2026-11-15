package com.rommelguerra.prototipomusichaptic.haptics;

/** Monophonic YIN, A4=440 Hz. Reuses work arrays; called only on the audio thread.
 * Input PCM16, 16000 Hz, 2048 samples. dBFS is relative, never calibrated dB SPL.
 */
public final class PitchDetector {
    public static final int SAMPLE_RATE = 16000, FRAME_SIZE = 2048;
    private static final int MIN_TAU = 15, MAX_TAU = 246, WINDOW = FRAME_SIZE / 2;
    private final double[] samples = new double[FRAME_SIZE];
    private final double[] difference = new double[MAX_TAU+1];
    public static final class Result {
        public final double frequency, dbfs, confidence;
        public final int midi;
        Result(double hz, double db, double confidence) {
            frequency = hz; dbfs = db; this.confidence = confidence;
            midi = hz > 0 ? (int)Math.round(69+12*Math.log(hz/440)/Math.log(2)) : -1;
        }
    }
    public Result analyze(short[] pcm, double thresholdDbfs) {
        if (pcm.length != FRAME_SIZE) throw new IllegalArgumentException("2048 muestras requeridas");
        double mean=0, energy=0;
        for (short v:pcm) mean+=v;
        mean/=FRAME_SIZE;
        int clipped=0;
        for (int i=0;i<FRAME_SIZE;i++) {
            samples[i]=(pcm[i]-mean)/32768.0; energy+=samples[i]*samples[i];
            if (Math.abs((int)pcm[i])>=32700) clipped++;
        }
        double db=20*Math.log10(Math.max(1e-9,Math.sqrt(energy/FRAME_SIZE)));
        if (db<thresholdDbfs || clipped>FRAME_SIZE/10) return new Result(0,db,0);
        difference[0]=1;
        double sum=0;
        for (int tau=1;tau<=MAX_TAU;tau++) {
            double d=0;
            for (int i=0;i<WINDOW;i++) { double delta=samples[i]-samples[i+tau]; d+=delta*delta; }
            sum+=d;
            difference[tau]=sum>0 ? d*tau/sum : 1;
        }
        int tau=MIN_TAU;
        while (tau<MAX_TAU && difference[tau]>=0.15) tau++;
        if (tau>=MAX_TAU) return new Result(0,db,0);
        while (tau+1<=MAX_TAU && difference[tau+1]<difference[tau]) tau++;
        double refined=tau;
        if (tau>1 && tau<MAX_TAU) {
            double a=difference[tau-1], b=difference[tau], c=difference[tau+1];
            double divisor=2*(a-2*b+c);
            if (Math.abs(divisor)>1e-12) refined+=(a-c)/divisor;
        }
        double hz=SAMPLE_RATE/refined;
        if (!Double.isFinite(hz) || hz<65 || hz>1050) return new Result(0,db,0);
        return new Result(hz,db,1-difference[tau]);
    }
}
