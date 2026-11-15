package com.rommelguerra.prototipomusichaptic.ui.viewmodels

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rommelguerra.prototipomusichaptic.data.BaseDatosHelper
import com.rommelguerra.prototipomusichaptic.haptics.*
import com.rommelguerra.prototipomusichaptic.models.NoteModel
import com.rommelguerra.prototipomusichaptic.services.HapticService
import kotlinx.coroutines.*

class MainMenuViewModel(application: Application) : AndroidViewModel(application) {
    private val hapticService = HapticService(application)
    private var feedbackJob: Job? = null
    private val lifecycleStop: () -> Unit = { stopScan(); clearFeedback() }
    var userName by mutableStateOf("Usuario"); private set
    val notes = mutableStateListOf<NoteModel>()
    val scannedDevices = mutableStateListOf<BluetoothDevice>()
    var isScanning by mutableStateOf(false); private set
    var isConnecting by mutableStateOf(false); private set
    var isConnected by mutableStateOf(false); private set
    var statusText by mutableStateOf("Desconectado"); private set
    var lastAck by mutableStateOf("Sin órdenes confirmadas"); private set
    var selectedNote by mutableStateOf<String?>(null); private set
    var feedbackText by mutableStateOf(""); private set

    init {
        HapticRuntime.register(lifecycleStop)
        viewModelScope.launch {
            HapticConnection.state.collect {
                isScanning = it.scanning; isConnecting = it.connecting
                isConnected = it.ready; statusText = it.message; lastAck = it.lastAck
            }
        }
        viewModelScope.launch {
            HapticConnection.devices.collect { scannedDevices.clear(); scannedDevices.addAll(it) }
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val db = BaseDatosHelper(application)
                try {
                    val email = application.getSharedPreferences("SesionUsuario", Context.MODE_PRIVATE)
                        .getString("correoUsuario", "") ?: ""
                    (db.obtenerDatosPerfil(email)?.get("nombre") ?: "Usuario") to db.obtenerNotasMuestrario()
                } finally { db.close() }
            }
            userName = result.first
            notes.addAll(result.second.sortedBy { HapticProtocol.pitchClass(it.nombre) })
        }
    }
    fun startScan() { HapticConnection.scan(getApplication()) }
    fun stopScan() { HapticConnection.stopScan() }
    fun connectToDevice(device: BluetoothDevice, context: Context) { HapticConnection.connect(context, device) }
    fun disconnect() { stopAll(); HapticConnection.disconnect() }
    fun playNote(note: NoteModel) {
        hapticService.emitirTacton(note)
        feedbackJob?.cancel()
        selectedNote = note.nombre
        val pc = HapticProtocol.pitchClass(note.nombre)
        feedbackText = "Nota ${if (pc >= 0) HapticProtocol.NAMES[pc] else note.nombre}"
        feedbackJob = viewModelScope.launch {
            delay(note.duracionMs.coerceIn(300, 1500).toLong())
            selectedNote = null; feedbackText = ""
        }
    }
    fun testMotor(left: Boolean, right: Boolean) { clearFeedback(); hapticService.probarLado(left, right) }
    private fun clearFeedback() { feedbackJob?.cancel(); selectedNote = null; feedbackText = "" }
    fun stopAll() { clearFeedback(); hapticService.detenerSecuencia(); HapticConnection.stop() }
    fun leaveScreen() { stopScan(); stopAll() }
    override fun onCleared() {
        leaveScreen(); hapticService.liberar(); HapticRuntime.unregister(lifecycleStop)
        super.onCleared()
    }
}
