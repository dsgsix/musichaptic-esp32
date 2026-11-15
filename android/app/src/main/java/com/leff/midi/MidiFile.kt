package com.leff.midi

import com.leff.midi.event.*
import java.io.InputStream
import java.io.ByteArrayOutputStream

/** Bounded Standard MIDI File 0/1 reader; PPQ timing, channel notes and tempo changes. */
class MidiFile(inputStream: InputStream) {
    val tracks = mutableListOf<MidiTrack>()
    val ticksPorNegra: Int
    val formato: Int
    init {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while(true) {
            val count = inputStream.read(buffer); if(count < 0) break
            require(output.size()+count <= 8*1024*1024) { "MIDI mayor a 8 MB" }
            output.write(buffer,0,count)
        }
        val data = output.toByteArray()
        val cursor = Cursor(data)
        require(cursor.ascii(4) == "MThd") { "Falta cabecera MIDI" }
        val size = cursor.int32()
        require(size >= 6) { "Cabecera MIDI incompleta" }
        formato = cursor.int16(); val count = cursor.int16(); ticksPorNegra = cursor.int16()
        require(formato in 0..1 && count in 1..128 && (formato != 0 || count == 1)) { "Solo MIDI 0/1" }
        require(ticksPorNegra in 1..32767) { "Se requiere tiempo PPQ; SMPTE no admitido" }
        cursor.skip(size-6)
        var totalEvents = 0
        repeat(count) {
            require(cursor.ascii(4) == "MTrk") { "Falta pista MIDI" }
            val trackBytes = cursor.bytes(cursor.int32())
            val t = Cursor(trackBytes); val track = MidiTrack()
            var ticks = 0L; var runningStatus = 0
            while(t.remaining > 0) {
                require(++totalEvents <= 200000) { "Demasiados eventos MIDI" }
                ticks += t.varlen(); track.endTick = ticks
                var status = t.peek()
                if(status < 128) { require(runningStatus in 0x80..0xef) { "Running status inválido" }; status = runningStatus }
                else { t.byte(); runningStatus = if(status < 0xf0) status else 0 }
                val channel = status and 15
                when {
                    status == 0xff -> {
                        val type = t.byte(); val bytes = t.bytes(t.varlen())
                        if(type == 0x51) {
                            require(bytes.size == 3)
                            val tempo = ((bytes[0].toInt() and 255) shl 16) or ((bytes[1].toInt() and 255) shl 8) or (bytes[2].toInt() and 255)
                            require(tempo > 0); track.events.add(MetaTempo(ticks,tempo))
                        }
                        if(type == 0x2f) break
                    }
                    status == 0xf0 || status == 0xf7 -> t.skip(t.varlen())
                    status in 0x80..0xef -> when(status and 0xf0) {
                        0x80,0x90 -> {
                            val note = t.dataByte(); val velocity = t.dataByte()
                            track.events.add(if(status and 0xf0 == 0x80 || velocity == 0) NoteOff(ticks,channel,note,velocity)
                                else NoteOn(ticks,channel,note,velocity))
                        }
                        0xc0,0xd0 -> t.dataByte()
                        else -> { t.dataByte(); t.dataByte() }
                    }
                    else -> error("Evento MIDI no admitido")
                }
            }
            tracks.add(track)
        }
    }
    private class Cursor(private val data: ByteArray) {
        private var index = 0
        val remaining get() = data.size-index
        fun peek(): Int { require(remaining > 0) { "MIDI truncado" }; return data[index].toInt() and 255 }
        fun byte() = peek().also { index++ }
        fun dataByte() = byte().also { require(it < 128) { "Dato MIDI inválido" } }
        fun skip(n: Int) { require(n >= 0 && n <= remaining) { "Longitud MIDI inválida" }; index += n }
        fun bytes(n: Int): ByteArray { require(n >= 0 && n <= remaining) { "MIDI truncado" }; return data.copyOfRange(index,index+n).also { index += n } }
        fun ascii(n: Int) = String(bytes(n),Charsets.US_ASCII)
        fun int16() = (byte() shl 8) or byte()
        fun int32(): Int { val value = (byte().toLong() shl 24) or (byte().toLong() shl 16) or (byte().toLong() shl 8) or byte().toLong(); require(value <= Int.MAX_VALUE); return value.toInt() }
        fun varlen(): Int {
            var value = 0
            repeat(4) { val b = byte(); value = (value shl 7) or (b and 127); if(b and 128 == 0) return value }
            error("VLQ MIDI demasiado largo")
        }
    }
}
class MidiTrack {
    val events = mutableListOf<MidiEvent>()
    var endTick = 0L
}
