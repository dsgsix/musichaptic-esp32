package com.rommelguerra.prototipomusichaptic.haptics

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.rommelguerra.prototipomusichaptic.models.AppGlobals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HapticLinkState(
    val ready: Boolean = false,
    val centerAvailable: Boolean = false,
    val musicalAvailable: Boolean = false,
    val levelControlAvailable: Boolean = false,
    val protocolVersion: Int = 1,
    val scanning: Boolean = false,
    val connecting: Boolean = false,
    val message: String = "Desconectado",
    val lastAck: String = "Sin órdenes confirmadas"
)

/** One GATT owner shared by every screen; call public methods on main. */
object HapticConnection {
    private var client: BleHapticClient? = null
    private val _state = MutableStateFlow(HapticLinkState())
    val state = _state.asStateFlow()
    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices = _devices.asStateFlow()

    fun initialize(context: Context) {
        if (client != null) return
        client = BleHapticClient(context.applicationContext, object : BleHapticClient.Listener {
            override fun onState(message: String, ready: Boolean) {
                val becameReady = ready && !_state.value.ready
                AppGlobals.estaBrazaleteConectado = ready
                if (becameReady) AppGlobals.dispositivoSalida = "BRAZALETE"
                _state.value = _state.value.copy(ready = ready,
                    centerAvailable = client?.isCenterAvailable == true,
                    musicalAvailable = client?.isMusicalAvailable == true,
                    levelControlAvailable = client?.isLevelControlAvailable == true,
                    protocolVersion = client?.protocolVersion ?: 1,
                    scanning = client?.isScanning == true,
                    connecting = client?.isConnecting == true, message = message,
                    lastAck = if (!ready) "Sin órdenes confirmadas" else _state.value.lastAck)
            }
            override fun onDevice(device: BluetoothDevice, name: String) {
                if (_devices.value.none { it.address == device.address }) _devices.value += device
            }
            override fun onAck(ack: HapticProtocol.Ack) {
                val message = if (ack.result == HapticProtocol.STALE_COMMAND)
                    "Orden #${ack.sequence} descartada por llegar tarde"
                else "Orden #${ack.sequence}: base izq ${ack.left}% · der ${ack.right}% · ${ack.duration} ms" +
                    (if (ack.version == 4) " · intensidad ${ack.intensity}%" else "") +
                    (if (ack.carrierCentiHz > 0) " · señal ${ack.carrierCentiHz / 100.0} Hz" else "")
                _state.value = _state.value.copy(lastAck = message)
            }
        })
    }
    fun scan(context: Context) { initialize(context); _devices.value = emptyList(); client?.scan() }
    fun stopScan() { client?.cancelScan() }
    fun connect(context: Context, device: BluetoothDevice) { initialize(context); client?.connect(device) }
    fun disconnect() { client?.disconnect() }
    fun send(left: Int, right: Int, durationMs: Int): Boolean =
        client?.send(left, right, durationMs) == true

    fun levelsForNote(name: String, position: Double, intensity: Int,
                      musical: Boolean = _state.value.musicalAvailable): IntArray {
        val pc = HapticProtocol.pitchClass(name)
        return if (musical && pc >= 0) HapticProtocol.musicalChannels(pc, intensity)
        else HapticProtocol.channelsForNote(name, position, intensity)
    }

    fun sendNote(midi: Int, amplitude: Int, durationMs: Int, intensity: Int = 100): Boolean {
        val levels = if (_state.value.musicalAvailable) HapticProtocol.musicalChannels(midi, amplitude)
            else HapticProtocol.channels(midi, amplitude)
        return sendControlled(levels[0], levels[1],
            HapticProtocol.NOTE_CENTI_HZ[Math.floorMod(midi, 12)], intensity, durationMs)
    }
    fun sendControlled(left: Int, right: Int, centiHz: Int, intensity: Int, durationMs: Int): Boolean {
        require(intensity in 0..100)
        if (intensity == 0 || (left == 0 && right == 0)) { stop(); return _state.value.ready }
        if (_state.value.levelControlAvailable && centiHz > 0)
            return client?.sendControlled(left, right, centiHz, intensity, durationMs) == true
        val l = (left * intensity + 50) / 100
        val r = (right * intensity + 50) / 100
        return if (_state.value.musicalAvailable && centiHz > 0)
            client?.sendMusical(l, r, 0, centiHz, durationMs) == true
        else send(l, r, durationMs)
    }
    fun stop() { client?.stop() }
}
