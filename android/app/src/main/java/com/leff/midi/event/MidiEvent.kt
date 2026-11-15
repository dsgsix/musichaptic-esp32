package com.leff.midi.event

/**
 * Evento base de un archivo MIDI, con el tiempo absoluto
 * (en ticks desde el inicio del track) en el que ocurre.
 */
abstract class MidiEvent(val ticks: Long)

/**
 * Note On. Si `velocity` es 0 se debe tratar como Note Off
 * (algunos DAW usan esta variante en vez del 0x8n).
 */
class NoteOn(
    ticks: Long,
    val channel: Int,
    val note: Int,
    val velocity: Int
) : MidiEvent(ticks)

/**
 * Note Off.
 */
class NoteOff(
    ticks: Long,
    val channel: Int,
    val note: Int,
    val velocity: Int
) : MidiEvent(ticks)

/**
 * Meta-evento Set Tempo (0xFF 0x51 0x03).
 * `microsPerQuarter` = microsegundos por negra.
 * 500000 = 120 BPM (default MIDI).
 */
class MetaTempo(
    ticks: Long,
    val microsPerQuarter: Int
) : MidiEvent(ticks)
