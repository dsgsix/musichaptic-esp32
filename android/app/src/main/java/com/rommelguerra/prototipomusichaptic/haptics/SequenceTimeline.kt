package com.rommelguerra.prototipomusichaptic.haptics

import com.rommelguerra.prototipomusichaptic.models.NoteModel
import org.json.JSONObject
import kotlin.math.roundToLong

data class ScheduledNote(val startMs: Long, val note: NoteModel)

object SequenceTimeline {
    fun parse(json: String): List<ScheduledNote> {
        val root = JSONObject(json)
        val array = root.getJSONArray("secuencia")
        require(array.length() <= 50000) { "Secuencia demasiado larga" }
        // Old bundled songs had every t=0. Preserve their sequential ordering.
        val timed = root.optInt("version",1) >= 2 || (0 until array.length()).any { array.getJSONObject(it).optDouble("t", 0.0) > 0.0 }
        var cursor = 0L
        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            val duration = item.getLong("d").coerceIn(1, 30000).toInt()
            val seconds = item.optDouble("t", 0.0)
            val hz = item.getDouble("f")
            val position = item.getDouble("u")
            require(seconds.isFinite() && seconds in 0.0..7200.0 && hz.isFinite() && hz > 0 && position.isFinite())
            val start = if (timed) (seconds * 1000).roundToLong() else cursor
            cursor = start + duration
            val amplitude = item.optInt("a", 255).coerceIn(0, 255)
            ScheduledNote(start, NoteModel(item.getString("n"), "", longArrayOf(0, duration.toLong()),
                intArrayOf(0, amplitude), hz, position.toFloat().coerceIn(0f, 10f),
                (amplitude * 100 + 127) / 255, duration))
        }.sortedBy { it.startMs }
    }
}
