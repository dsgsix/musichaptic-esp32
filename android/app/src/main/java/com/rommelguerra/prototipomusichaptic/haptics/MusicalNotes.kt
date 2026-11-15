package com.rommelguerra.prototipomusichaptic.haptics

import kotlin.math.log2
import kotlin.math.roundToInt

object MusicalNotes {
    val names = listOf("DO", "DO#", "RE", "RE#", "MI", "FA", "FA#", "SOL", "SOL#", "LA", "LA#", "SI")
    fun midi(frequency: Double): Int? = if (frequency.isFinite() && frequency in 20.0..5000.0)
        (69 + 12 * log2(frequency / 440.0)).roundToInt() else null
    fun name(midi: Int): String = names[Math.floorMod(midi, 12)]
}
