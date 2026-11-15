package com.rommelguerra.prototipomusichaptic.haptics

import com.rommelguerra.prototipomusichaptic.algoritmo.NotaHaptica

/** Musical pitch determines the note; these frequencies are only the original haptic carrier table. */
object HapticMapping {
    private val carriers = doubleArrayOf(62.15,75.82,92.50,112.85,137.68,167.97,204.92,250.0,305.0,372.10,453.96,553.83)
    private val amplitudes = intArrayOf(255,245,240,235,230,225,220,225,230,235,245,255)
    fun fromMidi(midi: Int, startSeconds: Double, durationMs: Long, velocity: Int = 127): NotaHaptica {
        require(midi in 0..127 && startSeconds.isFinite() && startSeconds >= 0)
        val pc = midi % 12
        return NotaHaptica(MusicalNotes.name(midi),carriers[pc],pc * 10.0 / 11.0,
            (amplitudes[pc] * velocity.coerceIn(0,127) / 127.0).toInt(),durationMs.coerceIn(1,30000),startSeconds)
    }
}
