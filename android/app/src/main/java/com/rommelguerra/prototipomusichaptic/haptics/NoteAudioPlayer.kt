package com.rommelguerra.prototipomusichaptic.haptics

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** One coroutine owns one mono track. Cancellation always stops and releases it. */
object NoteAudioPlayer {
    suspend fun play(pitchClass: Int, durationMs: Int) {
        val pcm = withContext(Dispatchers.Default) { NoteTone.pcm(pitchClass, durationMs) }
        currentCoroutineContext().ensureActive()
        var track: AudioTrack? = null
        try {
            val output = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(NoteTone.SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.size * 2).build()
            track = output
            check(output.state == AudioTrack.STATE_INITIALIZED) { "Audio no disponible" }
            check(output.write(pcm, 0, pcm.size) == pcm.size) { "Audio incompleto" }
            output.setVolume(1f) // Does not change Android's media volume or haptic intensity.
            output.play()
            delay(durationMs.toLong() + 40L)
        } catch (e: CancellationException) {
            throw e
        } catch (e: RuntimeException) {
            Log.w("NoteAudio", "No se pudo reproducir la nota", e)
        } finally {
            track?.let { output ->
                try { output.stop() } catch (_: IllegalStateException) { }
                output.release()
            }
        }
    }
}
