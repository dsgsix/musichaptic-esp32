package com.rommelguerra.prototipomusichaptic.haptics

import android.content.Context
import android.content.SharedPreferences
import com.rommelguerra.prototipomusichaptic.models.AppGlobals

/** Initialize on the main thread before creating playback services. */
object HapticPreferences {
    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        if (preferences != null) return
        val saved = context.applicationContext.getSharedPreferences("HapticPlayback", Context.MODE_PRIVATE)
        preferences = saved
        val intensity = saved.getString("intensity", "ALTA") ?: "ALTA"
        AppGlobals.intensidadSeleccionada = if (HapticIntensity.valid(intensity)) intensity else "ALTA"
        // One-time migration: the previous default was silent while connected.
        if (!saved.getBoolean("audibleNotesV25", false)) {
            saved.edit().putBoolean("noteAudio", true).putBoolean("audibleNotesV25", true).apply()
        }
        AppGlobals.reproducirWavConBrazalete = saved.getBoolean("noteAudio", true)
    }

    fun chooseIntensity(value: String) {
        require(HapticIntensity.valid(value))
        if (value == AppGlobals.intensidadSeleccionada) return
        AppGlobals.intensidadSeleccionada = value
        preferences?.edit()?.putString("intensity", value)?.apply()
    }

    fun chooseNoteAudio(value: Boolean) {
        HapticRuntime.stopAll()
        AppGlobals.reproducirWavConBrazalete = value
        preferences?.edit()?.putBoolean("noteAudio", value)?.apply()
    }
}
