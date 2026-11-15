package com.rommelguerra.prototipomusichaptic.haptics;

/** Bounded latest-value queue. A pending STOP always precedes the latest subsequent PLAY. */
public final class HapticCommandQueue {
    public static final class Entry {
        public final int left,right,center,hz,ms,carrierCentiHz,intensity;
        public final long created;
        Entry(int left,int right,int center,int hz,int ms,long created,int carrierCentiHz,int intensity) { this.intensity=intensity; this.carrierCentiHz=carrierCentiHz; this.left=left;this.right=right;this.center=center;this.hz=hz;this.ms=ms;this.created=created; }
        boolean isStop() { return left==0 && right==0 && center==0; }
    }
    private Entry pending,afterStop;
    public void offer(int left,int right,int ms,long now) {
        offer(left,right,0,0,ms,now);
    }
    public void offer(int left,int right,int center,int hz,int ms,long now) {
        offer(left,right,center,hz,ms,now,0);
    }
    public void offer(int left,int right,int center,int hz,int ms,long now,int carrierCentiHz) {
        offer(left,right,center,hz,ms,now,carrierCentiHz,100);
    }
    public void offer(int left,int right,int center,int hz,int ms,long now,int carrierCentiHz,int intensity) {
        Entry next = new Entry(left,right,center,hz,ms,now,carrierCentiHz,intensity);
        if(next.isStop()) { pending=next;afterStop=null; }
        else if(pending!=null && pending.isStop()) afterStop=next;
        else pending=next;
    }
    public Entry poll(long now) {
        while(pending!=null) {
            Entry next=pending;pending=afterStop;afterStop=null;
            if(next.isStop()) return new Entry(0,0,0,0,0,next.created,0,0);
            long age=Math.max(0,now-next.created);
            // Duration is measured from offer(), not from a delayed BLE write.
            if(age<=250 && age<next.ms)
                return new Entry(next.left,next.right,next.center,next.hz,next.ms-(int)age,next.created,next.carrierCentiHz,next.intensity);
        }
        return null;
    }
    public void clear() { pending=null;afterStop=null; }
}
