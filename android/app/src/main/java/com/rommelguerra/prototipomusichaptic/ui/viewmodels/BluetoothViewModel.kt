package com.rommelguerra.prototipomusichaptic.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.rommelguerra.prototipomusichaptic.haptics.HapticConnection

/** Compatibility read-only view; connection readiness comes from the firmware handshake. */
class BluetoothViewModel : ViewModel() {
    val state = HapticConnection.state
    val isConnected get() = state.value.ready
    val deviceName get() = state.value.message
}
