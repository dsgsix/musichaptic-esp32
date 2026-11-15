package com.rommelguerra.prototipomusichaptic.services

import android.content.ContentResolver
import android.net.Uri
import com.leff.midi.MidiFile
import com.leff.midi.event.*
import com.rommelguerra.prototipomusichaptic.algoritmo.NotaHaptica
import com.rommelguerra.prototipomusichaptic.haptics.HapticMapping
import java.io.InputStream
import kotlin.math.roundToLong

/** Highest note per onset group (30 ms); musical pitch class is preserved across octaves. */
object MidiImportService {
    fun importar(uri: Uri, contentResolver: ContentResolver): List<NotaHaptica>? =
        contentResolver.openInputStream(uri)?.use { importarDesdeStream(it) }

    fun importarDesdeStream(stream: InputStream): List<NotaHaptica>? {
        val midi = MidiFile(stream)
        data class Tempo(val tick: Long, val ms: Double, val microsPerQuarter: Int)
        val tempos = mutableListOf(Tempo(0,0.0,500000))
        midi.tracks.flatMap { it.events }.filterIsInstance<MetaTempo>().sortedBy { it.ticks }.forEach {
            val last = tempos.last()
            val ms = last.ms + (it.ticks-last.tick)*last.microsPerQuarter/1000.0/midi.ticksPorNegra
            if(it.ticks == last.tick) tempos.removeAt(tempos.lastIndex)
            tempos.add(Tempo(it.ticks,ms,it.microsPerQuarter))
        }
        fun milliseconds(tick: Long): Double {
            var lo = 0; var hi = tempos.lastIndex
            while(lo < hi) { val mid = (lo+hi+1)/2; if(tempos[mid].tick <= tick) lo=mid else hi=mid-1 }
            val tempo = tempos[lo]
            return tempo.ms + (tick-tempo.tick)*tempo.microsPerQuarter/1000.0/midi.ticksPorNegra
        }
        data class Played(val start: Double, val end: Double, val pitch: Int, val velocity: Int)
        val all = mutableListOf<Played>()
        for(track in midi.tracks) {
            val opened = mutableMapOf<Pair<Int,Int>,NoteOn>()
            fun close(key: Pair<Int,Int>, endTick: Long) {
                val start = opened.remove(key) ?: return
                if(endTick > start.ticks) all.add(Played(milliseconds(start.ticks),milliseconds(endTick),start.note,start.velocity))
                require(all.size <= 50000) { "MIDI con más de 50 000 notas" }
            }
            for(event in track.events) when(event) {
                is NoteOn -> if(event.channel != 9) {
                    val key = event.channel to event.note
                    close(key,event.ticks); opened[key] = event
                }
                is NoteOff -> if(event.channel != 9) close(event.channel to event.note,event.ticks)
            }
            opened.keys.toList().forEach { close(it,track.endTick) }
        }
        if(all.isEmpty()) return null
        val sorted = all.sortedBy { it.start }
        val mono = mutableListOf<Played>()
        var i = 0
        while(i < sorted.size) {
            val start = sorted[i].start; var chosen = sorted[i]; var j=i+1
            while(j < sorted.size && sorted[j].start-start < 30) { if(sorted[j].pitch > chosen.pitch) chosen=sorted[j]; j++ }
            mono.add(chosen); i=j
        }
        require(mono.last().end <= 7_200_000) { "MIDI de más de dos horas" }
        return mono.mapIndexed { index,note ->
            val end = minOf(note.end,mono.getOrNull(index+1)?.start ?: note.end)
            HapticMapping.fromMidi(note.pitch,note.start/1000,(end-note.start).roundToLong().coerceIn(1,30000),note.velocity)
        }
    }
}
