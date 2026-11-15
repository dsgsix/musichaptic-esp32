package com.rommelguerra.prototipomusichaptic.services

import android.content.Context
import android.media.*
import android.os.*
import android.util.Log
import com.rommelguerra.prototipomusichaptic.haptics.*
import com.rommelguerra.prototipomusichaptic.models.AppGlobals
import com.rommelguerra.prototipomusichaptic.models.NoteModel
import kotlinx.coroutines.*
import org.json.JSONObject
import kotlin.math.*

/** One cancellable playback owner per screen. All BLE writes go through HapticConnection. */
class HapticService(context: Context) {
    private val context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val vibrator = if (Build.VERSION.SDK_INT >= 31)
        (this.context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    else @Suppress("DEPRECATION") (this.context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
    private var audioJob: Job? = null
    private var pulseJob: Job? = null
    private var sequenceJob: Job? = null
    private var sequenceGeneration = 0
    private var ownedBle = false
    private var closed = false
    private val lifecycleStop: () -> Unit = { detenerSecuencia() }
    init { HapticPreferences.initialize(this.context); HapticConnection.initialize(this.context); HapticRuntime.register(lifecycleStop) }

    private fun factor() = HapticIntensity.percent(AppGlobals.intensidadSeleccionada) / 100f
    private fun channels(note: NoteModel, intensity: Float, musical: Boolean = HapticConnection.state.value.musicalAvailable): Pair<Int,Int> {
        val amp = (note.amplitudBrazalete.coerceIn(0,100) * intensity).roundToInt()
        val ch = HapticConnection.levelsForNote(note.nombre, note.ubicacion.toDouble(), amp, musical)
        return ch[0] to ch[1]
    }
    fun emitirTacton(nota: NoteModel) = emitirTacton(nota, controlBle = true)

    private fun emitirTacton(nota: NoteModel, controlBle: Boolean) {
        if (closed) return
        val duration = nota.duracionMs.coerceIn(1,30000)
        stopNote(sendStop = AppGlobals.dispositivoSalida != "BRAZALETE")
        val factor = factor()
        if (AppGlobals.dispositivoSalida == "BRAZALETE") {
            val (left,right) = channels(nota,1f)
            if (controlBle && HapticConnection.state.value.ready) {
                ownedBle = true
                pulseJob = scope.launch {
                    val until = SystemClock.uptimeMillis() + duration
                    do {
                        val remaining = (until - SystemClock.uptimeMillis()).toInt()
                        if (remaining <= 0 || !HapticConnection.state.value.ready) break
                        val pc = HapticProtocol.pitchClass(nota.nombre)
                        if (pc >= 0) HapticConnection.sendNote(pc,
                            nota.amplitudBrazalete.coerceIn(0,100), minOf(remaining,500),
                            HapticIntensity.percent(AppGlobals.intensidadSeleccionada))
                        else HapticConnection.sendControlled(left,right,25000,
                            HapticIntensity.percent(AppGlobals.intensidadSeleccionada),minOf(remaining,500))
                        delay(minOf(remaining,250).toLong())
                    } while (isActive)
                }
            }
        } else if (vibrator?.hasVibrator() == true) {
            try {
                val times = nota.patronVibracion.map { it.coerceIn(0,30000) }.toLongArray()
                if (times.any { it > 0 }) {
                    if (Build.VERSION.SDK_INT >= 26) {
                        val amps = IntArray(times.size) { ((nota.amplitudes.getOrElse(it) { 0 }).coerceIn(0,255) * factor).roundToInt() }
                        vibrator.vibrate(VibrationEffect.createWaveform(times,amps,-1))
                    } else @Suppress("DEPRECATION") vibrator.vibrate(times,-1)
                }
            } catch (e: RuntimeException) { Log.w("HapticService","Vibración no disponible",e) }
        }
        if (AppGlobals.dispositivoSalida != "BRAZALETE" || AppGlobals.reproducirWavConBrazalete) {
            val pc = HapticProtocol.pitchClass(nota.nombre)
            if (pc >= 0) audioJob = scope.launch { NoteAudioPlayer.play(pc, duration) }
        }
    }
    fun probarLado(left: Boolean, right: Boolean) {
        detenerSecuencia()
        // Same frequency and intensity path as notes. SOL is the reference (250 Hz).
        ownedBle = HapticConnection.sendControlled(if(left) 100 else 0,if(right) 100 else 0,
            25000,HapticIntensity.percent(AppGlobals.intensidadSeleccionada),500)
    }
    fun enviarComandoBle(jsonPayload: String) {
        val obj = JSONObject(jsonPayload)
        val duration = obj.getLong("d").coerceIn(1,30000).toInt()
        emitirTacton(NoteModel(obj.getString("n"),"",longArrayOf(0,duration.toLong()),
            intArrayOf(0,obj.optInt("a",255).coerceIn(0,255)),obj.getDouble("f"),
            obj.getDouble("u").toFloat(),(obj.optInt("a",255).coerceIn(0,255)*100+127)/255,duration))
    }
    fun reproducirSecuencia(tramaJson: String, onFinished: () -> Unit = {}, onError: (String) -> Unit = {}) {
        detenerSecuencia()
        val token = sequenceGeneration
        sequenceJob = scope.launch {
            try {
                val events = withContext(Dispatchers.Default) { SequenceTimeline.parse(tramaJson) }
                if (AppGlobals.dispositivoSalida == "BRAZALETE") {
                    check(HapticConnection.state.value.ready) { "Conecta el brazalete antes de reproducir." }
                    val musical = HapticConnection.state.value.musicalAvailable
                    val segments = withContext(Dispatchers.Default) {
                        HapticTimeline.mix(events.map { event ->
                            val (left, right) = channels(event.note, 1f, musical)
                            val pc = HapticProtocol.pitchClass(event.note.nombre)
                            val hz = if (pc >= 0) HapticProtocol.NOTE_CENTI_HZ[pc] else 25000
                            HapticTimeline.Note(event.startMs, event.note.duracionMs.toLong(), left, right, hz)
                        })
                    }
                    val started = SystemClock.uptimeMillis()
                    // The BLE timeline owns both channels; optional audio cannot cancel a held note.
                    coroutineScope {
                        launch { reproducirCanales(segments, started) }
                        if (AppGlobals.reproducirWavConBrazalete)
                            reproducirNotas(events, started, controlBle = false)
                    }
                } else {
                    reproducirNotas(events, SystemClock.uptimeMillis(), controlBle = true)
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { onError(e.message ?: "Secuencia inválida") }
            finally { if(token == sequenceGeneration) { stopNote(sendStop = true); onFinished() } }
        }
    }
    private suspend fun reproducirNotas(events: List<ScheduledNote>, started: Long, controlBle: Boolean) {
        for (event in events) {
            delay(maxOf(0, event.startMs - (SystemClock.uptimeMillis() - started)))
            val remaining = event.startMs + event.note.duracionMs - (SystemClock.uptimeMillis() - started)
            if (remaining > 0) emitirTacton(event.note.copy(duracionMs = remaining.toInt()), controlBle)
        }
        val end = events.maxOfOrNull { it.startMs + it.note.duracionMs } ?: 0
        delay(maxOf(0, end - (SystemClock.uptimeMillis() - started)))
    }
    private suspend fun reproducirCanales(segments: List<HapticTimeline.Segment>, started: Long) {
        for (segment in segments) {
            delay(maxOf(0, segment.startMs - (SystemClock.uptimeMillis() - started)))
            while (true) {
                currentCoroutineContext().ensureActive()
                val remaining = segment.endMs - (SystemClock.uptimeMillis() - started)
                if (remaining <= 0) break
                check(AppGlobals.dispositivoSalida == "BRAZALETE") { "La salida cambió. Reproduce de nuevo." }
                check(HapticConnection.state.value.ready) { "Se perdió la conexión con el brazalete." }
                if (segment.left == 0 && segment.right == 0) {
                    HapticConnection.stop()
                    delay(remaining)
                } else {
                    val accepted = HapticConnection.sendControlled(segment.left, segment.right, segment.carrierCentiHz,
                        HapticIntensity.percent(AppGlobals.intensidadSeleccionada), minOf(remaining, 500L).toInt())
                    check(accepted) { "No se pudo enviar la vibración al brazalete." }
                    ownedBle = true
                    delay(minOf(remaining, 250L))
                }
            }
        }
    }
    private fun stopNote(sendStop: Boolean = false) {
        pulseJob?.cancel(); pulseJob = null
        audioJob?.cancel(); audioJob = null
        vibrator?.cancel()
        if (sendStop && ownedBle) HapticConnection.stop()
        if (sendStop) ownedBle = false
    }
    fun detenerSecuencia() { sequenceGeneration++; sequenceJob?.cancel(); sequenceJob = null; stopNote(sendStop = true) }
    fun liberar() {
        if (closed) return
        detenerSecuencia(); closed = true; scope.cancel()
        HapticRuntime.unregister(lifecycleStop)
    }
}
