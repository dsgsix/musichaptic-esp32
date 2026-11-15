package com.rommelguerra.prototipomusichaptic.ui.viewmodels

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rommelguerra.prototipomusichaptic.algoritmo.*
import com.rommelguerra.prototipomusichaptic.data.BaseDatosHelper
import com.rommelguerra.prototipomusichaptic.haptics.*
import com.rommelguerra.prototipomusichaptic.models.MidiCatalog
import com.rommelguerra.prototipomusichaptic.models.NoteRepository
import com.rommelguerra.prototipomusichaptic.services.*
import kotlinx.coroutines.*
import java.io.File

class FileModeViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val hapticService = HapticService(application)
    private val analizador = AnalizadorAudioKotlin()
    private var operation: Job? = null
    private var operationGeneration = 0
    private var listJob: Job? = null
    private var tramaJsonActual: String? = null
    private val lifecycleStop: () -> Unit = { stopSequence() }
    var fileName by mutableStateOf("Ninguno"); private set
    var statusText by mutableStateOf("Esperando archivo…"); private set
    var progress by mutableStateOf(0f); private set
    var isPlaying by mutableStateOf(false); private set
    var isAnalyzing by mutableStateOf(false); private set
    var canPlay by mutableStateOf(false); private set
    var savedMidis by mutableStateOf<List<UserMidiEntry>>(emptyList()); private set
    init { HapticRuntime.register(lifecycleStop) }

    fun refrescarMidisGuardados() {
        listJob?.cancel()
        listJob = viewModelScope.launch { savedMidis = withContext(Dispatchers.IO) { UserMidiStorage.listar(context) } }
    }
    fun eliminarMidiGuardado(filename: String) {
        viewModelScope.launch { withContext(Dispatchers.IO) { UserMidiStorage.eliminar(context,filename) }; refrescarMidisGuardados() }
    }
    private data class Loaded(val name: String, val json: String)
    private fun begin(label: String, action: suspend (Int) -> Loaded) {
        operationGeneration++; val token = operationGeneration
        operation?.cancel(); stopSequence()
        fileName = label; statusText = "Preparando archivo…"; progress = 0f
        canPlay = false; tramaJsonActual = null; isAnalyzing = true
        operation = viewModelScope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) { action(token) }
                if(token != operationGeneration) return@launch
                val count = withContext(Dispatchers.Default) { SequenceTimeline.parse(loaded.json).size }
                require(count > 0) { "No se encontraron notas claras. Prueba una melodía de un solo instrumento." }
                fileName = loaded.name; tramaJsonActual = loaded.json; canPlay = true; progress = 1f
                statusText = "Listo: $count notas. Pulsa reproducir."
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if(token == operationGeneration) { statusText = "Error: ${e.message ?: "No se pudo abrir el archivo"}"; progress = 0f; canPlay = false } }
            finally { if(token == operationGeneration) isAnalyzing = false }
        }
    }
    private fun <T> database(block: (BaseDatosHelper,Int) -> T): T {
        val db = BaseDatosHelper(context)
        try {
            val email = context.getSharedPreferences("SesionUsuario",Context.MODE_PRIVATE).getString("correoUsuario","") ?: ""
            return block(db,db.obtenerIdUsuario(email))
        } finally { db.close() }
    }
    private fun store(name: String, notes: List<NotaHaptica>?): Loaded {
        require(!notes.isNullOrEmpty()) { "Archivo vacío o sin notas reconocibles" }
        val json = analizador.exportarSecuenciaAJsonBaseDatos(name,notes)
        database { db,user -> db.registrarCancionConAnalisis(name,user,json) }
        return Loaded(name,json)
    }
    fun cargarMidiGuardado(entry: UserMidiEntry) = begin(entry.displayName) {
        val file = UserMidiStorage.obtenerArchivo(context,entry.filename) ?: error("El archivo ya no existe")
        store(entry.displayName,file.inputStream().use { MidiImportService.importarDesdeStream(it) })
    }
    fun loadMidiFromCatalog(slug: String) = begin("Catálogo MIDI") {
        val item = MidiCatalog.encontrarPorSlug(slug) ?: error("Pieza no encontrada")
        store(item.titulo,context.assets.open(item.assetPath).use { MidiImportService.importarDesdeStream(it) })
    }
    fun loadFromLibrary(songName: String) = begin(songName) {
        database { db,user ->
            val saved = db.obtenerTramaAnalizada(songName,user)
            if(saved != null) Loaded(songName,saved)
            else {
                val sequence = NoteRepository.obtenerSecuenciaCancion(songName,db)
                require(sequence.isNotEmpty()) { "No se encontró la pieza" }
                var time = 0.0
                val notes = sequence.map { note ->
                    val duration = note.patronVibracion.sum().coerceIn(1,30000)
                    NotaHaptica(note.nombre,note.frecuenciaHz,note.ubicacion.toDouble(),note.amplitudes.maxOrNull() ?: 255,duration,time)
                        .also { time += (duration+50)/1000.0 }
                }
                val json = analizador.exportarSecuenciaAJsonBaseDatos(songName,notes)
                db.registrarCancionConAnalisis(songName,user,json); Loaded(songName,json)
            }
        }
    }
    fun analyzeMidiFile(uri: Uri, contentResolver: ContentResolver) = begin("Importando MIDI") {
        val name = getFileName(uri,contentResolver)
        val notes = MidiImportService.importar(uri,contentResolver)
        require(!notes.isNullOrEmpty()) { "MIDI vacío o sin notas melódicas" }
        currentCoroutineContext().ensureActive()
        val saved = UserMidiStorage.guardar(context,uri,contentResolver)
        withContext(Dispatchers.Main) { refrescarMidisGuardados() }
        store(saved?.displayName ?: name,notes)
    }
    fun analyzeAudioFile(uri: Uri, contentResolver: ContentResolver, cacheDir: File) = begin("Analizando audio") { token ->
        val name = getFileName(uri,contentResolver)
        val file = File.createTempFile("haptica_",".audio",cacheDir)
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(65536); var total = 0L
                    while(true) {
                        currentCoroutineContext().ensureActive()
                        val n = input.read(buffer); if(n < 0) break
                        total += n; require(total <= 100L*1024*1024) { "Usa un audio de hasta 100 MB" }
                        output.write(buffer,0,n)
                    }
                }
            } ?: error("No se pudo leer el archivo seleccionado")
            val notes = analizador.analizar(file.absolutePath) { p ->
                withContext(Dispatchers.Main) { if(token == operationGeneration) { progress=p/100f; statusText="Analizando $name: $p%" } }
            }
            currentCoroutineContext().ensureActive(); store(name,notes)
        } finally { file.delete() }
    }
    fun playSequence() {
        val json = tramaJsonActual ?: return
        if(isPlaying || isAnalyzing || !canPlay) return
        isPlaying = true; statusText = "Reproduciendo…"
        var failed = false
        hapticService.reproducirSecuencia(json,
            onFinished = { isPlaying=false; if(!failed) statusText="Reproducción finalizada" },
            onError = { failed=true; statusText="Error: $it" })
    }
    fun stopSequence() {
        hapticService.detenerSecuencia()
        if(isPlaying) { isPlaying = false; statusText = "Reproducción detenida" }
    }
    fun leaveScreen() {
        stopSequence(); operationGeneration++; operation?.cancel(); operation = null
        if(isAnalyzing) { isAnalyzing=false; statusText="Análisis cancelado" }
    }
    private fun getFileName(uri: Uri, resolver: ContentResolver): String {
        resolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use {
            if(it.moveToFirst()) { val index=it.getColumnIndex(OpenableColumns.DISPLAY_NAME); if(index >= 0) return it.getString(index) }
        }
        return uri.lastPathSegment ?: "Archivo"
    }
    override fun onCleared() { leaveScreen(); hapticService.liberar(); HapticRuntime.unregister(lifecycleStop); super.onCleared() }
}
