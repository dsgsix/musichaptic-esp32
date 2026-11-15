package com.rommelguerra.prototipomusichaptic.algoritmo

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.rommelguerra.prototipomusichaptic.haptics.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToLong

/** Streaming native decoding. No BLE output while analyzing; playback uses the exported timestamps. */
class AnalizadorAudioKotlin {
    companion object {
        const val MAX_SECONDS = 1200.0
        fun convertirFrecuenciaANota(frecuenciaObservadaHz: Double, tiempoS: Double, duracionMs: Long): NotaHaptica? {
            val midi = MusicalNotes.midi(frecuenciaObservadaHz) ?: return null
            return HapticMapping.fromMidi(midi,tiempoS,duracionMs)
        }
    }
    suspend fun analizar(ruta: String, onProgreso: suspend (Int) -> Unit = {}): List<NotaHaptica> = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        var started = false
        val notes = mutableListOf<NotaHaptica>()
        try {
            extractor.setDataSource(ruta)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: error("El archivo no contiene una pista de audio")
            extractor.selectTrack(track)
            val inputFormat = extractor.getTrackFormat(track)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: error("Formato desconocido")
            val duration = if(inputFormat.containsKey(MediaFormat.KEY_DURATION)) inputFormat.getLong(MediaFormat.KEY_DURATION)/1e6 else 0.0
            require(duration <= MAX_SECONDS) { "Usa un audio de hasta 20 minutos" }
            var sampleRate = 0; var channels = 0; var encoding = AudioFormat.ENCODING_PCM_16BIT
            var stream: PcmPitchStream? = null
            var lastProgress = -1
            var activeMidi = -1
            var activeStart = 0.0
            var activeEnd = 0.0
            fun finishNote() {
                if(activeMidi >= 0 && activeEnd > activeStart) {
                    require(notes.size < 50000) { "Demasiadas notas" }
                    notes.add(HapticMapping.fromMidi(activeMidi,activeStart,((activeEnd-activeStart)*1000).roundToLong()))
                }
                activeMidi = -1
            }
            fun configure(format: MediaFormat) {
                val rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                val count = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                require(count in 1..8) { "Cantidad de canales no admitida" }
                if(stream != null) require(rate == sampleRate && count == channels) { "El audio cambia de formato a mitad del archivo" }
                sampleRate = rate; channels = count
                encoding = if(format.containsKey(MediaFormat.KEY_PCM_ENCODING)) format.getInteger(MediaFormat.KEY_PCM_ENCODING) else AudioFormat.ENCODING_PCM_16BIT
                if(stream == null) stream = PcmPitchStream(rate) { result, time ->
                    val midi = result.midi
                    if(midi < 0) finishNote()
                    else {
                        if(activeMidi < 0 || midi % 12 != activeMidi % 12 || time-activeStart >= 29.8) {
                            finishNote(); activeMidi = midi; activeStart = time
                        }
                        activeEnd = time + PitchDetector.FRAME_SIZE / 2.0 / PitchDetector.SAMPLE_RATE
                    }
                }
            }
            fun consume(buffer: ByteBuffer) {
                buffer.order(ByteOrder.LITTLE_ENDIAN)
                val bytes = when(encoding) {
                    AudioFormat.ENCODING_PCM_16BIT -> 2
                    AudioFormat.ENCODING_PCM_FLOAT, AudioFormat.ENCODING_PCM_32BIT -> 4
                    AudioFormat.ENCODING_PCM_24BIT_PACKED -> 3
                    AudioFormat.ENCODING_PCM_8BIT -> 1
                    else -> error("Codificación PCM no admitida: $encoding")
                }
                require(buffer.remaining() % (channels*bytes) == 0) { "Bloque PCM incompleto" }
                while(buffer.remaining() >= channels*bytes) {
                    var mono = 0.0
                    repeat(channels) {
                        mono += when(encoding) {
                            AudioFormat.ENCODING_PCM_16BIT -> buffer.short / 32768.0
                            AudioFormat.ENCODING_PCM_FLOAT -> buffer.float.toDouble()
                            AudioFormat.ENCODING_PCM_32BIT -> buffer.int / 2147483648.0
                            AudioFormat.ENCODING_PCM_24BIT_PACKED -> {
                                val value = (buffer.get().toInt() and 255) or ((buffer.get().toInt() and 255) shl 8) or (buffer.get().toInt() shl 16)
                                value / 8388608.0
                            }
                            else -> ((buffer.get().toInt() and 255)-128) / 128.0
                        }
                    }
                    stream!!.accept(mono/channels)
                }
            }
            suspend fun reportProgress() {
                currentCoroutineContext().ensureActive()
                val seconds = stream?.seconds() ?: 0.0
                require(seconds <= MAX_SECONDS) { "Usa un audio de hasta 20 minutos" }
                val progress = if(duration > 0) (seconds/duration*100).toInt().coerceIn(0,99) else 0
                if(progress != lastProgress) { lastProgress=progress; onProgreso(progress) }
            }
            if(mime == "audio/raw") {
                configure(inputFormat)
                val buffer = ByteBuffer.allocate(1024*1024)
                while(true) {
                    currentCoroutineContext().ensureActive(); buffer.clear()
                    val size = extractor.readSampleData(buffer,0)
                    if(size < 0) break
                    buffer.position(0); buffer.limit(size); consume(buffer)
                    extractor.advance(); reportProgress()
                }
            } else {
                val codec = MediaCodec.createDecoderByType(mime); decoder = codec
                // Do not change the source sample rate/channel count: downmix/resample actual PCM below.
                codec.configure(inputFormat,null,null,0); codec.start(); started = true
                var inputEnded = false; var outputEnded = false
                var lastOutputNs = System.nanoTime()
                val info = MediaCodec.BufferInfo()
                while(!outputEnded) {
                    currentCoroutineContext().ensureActive()
                    if(!inputEnded) {
                        val index = codec.dequeueInputBuffer(10000)
                        if(index >= 0) {
                            val buffer = codec.getInputBuffer(index) ?: error("Sin buffer de entrada")
                            buffer.clear()
                            val size = extractor.readSampleData(buffer,0)
                            if(size < 0) { inputEnded=true; codec.queueInputBuffer(index,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM) }
                            else { codec.queueInputBuffer(index,0,size,extractor.sampleTime,0); extractor.advance() }
                        }
                    }
                    val index = codec.dequeueOutputBuffer(info,10000)
                    if(index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) configure(codec.outputFormat)
                    else if(index >= 0) {
                        try {
                            if(info.size > 0 && (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                if(stream == null) configure(codec.outputFormat)
                                val buffer = codec.getOutputBuffer(index) ?: error("Sin buffer de salida")
                                buffer.position(info.offset); buffer.limit(info.offset+info.size); consume(buffer)
                            }
                            outputEnded = (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                        } finally { codec.releaseOutputBuffer(index,false) }
                        lastOutputNs = System.nanoTime(); reportProgress()
                    }
                    require(System.nanoTime()-lastOutputNs < 15_000_000_000L) { "El decodificador dejó de entregar audio" }
                }
            }
            finishNote(); onProgreso(100)
            notes
        } finally {
            if(started) try { decoder?.stop() } catch (_: Exception) {}
            decoder?.release(); extractor.release()
        }
    }
    fun exportarSecuenciaAJsonBaseDatos(nombreCancion: String, notas: List<NotaHaptica>): String {
        val array = JSONArray(); notas.forEach { array.put(it.toJson()) }
        return JSONObject().put("version",2).put("cancion",nombreCancion).put("total_notas",notas.size).put("secuencia",array).toString()
    }
}
