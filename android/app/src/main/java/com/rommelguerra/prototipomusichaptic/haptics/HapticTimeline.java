package com.rommelguerra.prototipomusichaptic.haptics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** MIDI overlaps become bounded dual-channel levels. No Android dependency. */
public final class HapticTimeline {
    private HapticTimeline() {}
    public static final class Note {
        public final long startMs, durationMs;
        public final int left,right,carrierCentiHz;
        public Note(long startMs,long durationMs,int left,int right) { this(startMs,durationMs,left,right,0); }
        public Note(long startMs,long durationMs,int left,int right,int carrierCentiHz) {
            if(startMs<0 || startMs>7_200_000 || durationMs<1 || durationMs>30_000
                    || left<0 || left>100 || right<0 || right>100
                    || (carrierCentiHz!=0 && (carrierCentiHz<6000 || carrierCentiHz>60000)))
                throw new IllegalArgumentException("Nota fuera de rango");
            this.carrierCentiHz=carrierCentiHz; this.startMs=startMs; this.durationMs=durationMs; this.left=left; this.right=right;
        }
    }
    public static final class Segment {
        public final long startMs,endMs;
        public final int left,right,carrierCentiHz;
        Segment(long startMs,long endMs,int left,int right,int carrierCentiHz) {
            this.carrierCentiHz=carrierCentiHz;
            this.startMs=startMs; this.endMs=endMs; this.left=left; this.right=right;
        }
    }
    private static final class Change {
        final Note note; final int sign; final long key;
        Change(Note note,int sign,long key) { this.note=note; this.sign=sign; this.key=key; }
    }
    /** With frequencies, latest onset wins (highest frequency for simultaneous onsets).
     * The hardware has one carrier for both sides; different pitches cannot be mixed.
     * Legacy level-only callers retain per-channel maximum. */
    public static List<Segment> mix(List<Note> notes) {
        if(notes.size()>50_000) throw new IllegalArgumentException("Demasiadas notas");
        TreeMap<Long,List<Change>> boundaries=new TreeMap<>();
        int index=0; boolean musical=false;
        for(Note n:notes) {
            musical |= n.carrierCentiHz>0;
            long key=n.startMs*4_000_000_000L + n.carrierCentiHz*60000L + index++;
            boundaries.computeIfAbsent(n.startMs,time -> new ArrayList<>()).add(new Change(n,1,key));
            boundaries.computeIfAbsent(n.startMs+n.durationMs,time -> new ArrayList<>()).add(new Change(n,-1,key));
        }
        List<Segment> output=new ArrayList<>();
        int[] leftCounts=new int[101],rightCounts=new int[101];
        int left=0,right=0,carrier=0; long previous=0;
        TreeMap<Long,Note> active=new TreeMap<>();
        for(Map.Entry<Long,List<Change>> boundary:boundaries.entrySet()) {
            long time=boundary.getKey();
            if(time>previous) {
                Segment last=output.isEmpty() ? null : output.get(output.size()-1);
                if(last!=null && last.left==left && last.right==right && last.carrierCentiHz==carrier)
                    output.set(output.size()-1,new Segment(last.startMs,time,left,right,carrier));
                else output.add(new Segment(previous,time,left,right,carrier));
            }
            for(Change change:boundary.getValue()) {
                leftCounts[change.note.left]+=change.sign;
                rightCounts[change.note.right]+=change.sign;
                if(change.sign>0) active.put(change.key,change.note); else active.remove(change.key);
            }
            left=100; right=100;
            while(left>0 && leftCounts[left]==0) left--;
            while(right>0 && rightCounts[right]==0) right--;
            carrier=0;
            if(musical) {
                Note selected=active.isEmpty()?null:active.lastEntry().getValue();
                left=selected==null?0:selected.left; right=selected==null?0:selected.right;
                carrier=(left==0 && right==0)?0:selected.carrierCentiHz;
            }
            previous=time;
        }
        return output;
    }
}
