package com.rommelguerra.prototipomusichaptic.haptics

/** Main-thread lifecycle hooks. Stops all output and recording when Activity leaves foreground. */
object HapticRuntime {
    private val stops = mutableSetOf<() -> Unit>()
    fun register(stop: () -> Unit) { stops += stop }
    fun unregister(stop: () -> Unit) { stops -= stop }
    fun stopAll() { stops.toList().forEach { it() }; HapticConnection.stop() }
}
