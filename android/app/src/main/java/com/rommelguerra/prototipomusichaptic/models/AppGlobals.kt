package com.rommelguerra.prototipomusichaptic.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object AppGlobals {
    // Configuración de usuario (Calibración)
    var intensidadSeleccionada: String by mutableStateOf("ALTA") // Valores: BAJA, MEDIA, ALTA
    var dispositivoSalida: String by mutableStateOf("CELULAR")    // Valores: CELULAR, BRAZALETE

    // Reproducir un tono musical mono de la nota por el parlante del celular
    // cuando la salida principal es el brazalete.
    //
    // - false: con brazalete solo háptica, sin audio de nota.
    // - true (por defecto): con brazalete también se escucha la nota musical.
    //
    // Cuando dispositivoSalida = "CELULAR" el tono siempre se reproduce.
    var reproducirWavConBrazalete: Boolean by mutableStateOf(true)

    // Bluetooth Low Energy (BLE) con ESP32
    var estaBrazaleteConectado: Boolean by mutableStateOf(false)

    // UUIDs según código Arduino (VIBRAFIL_ESP32)
    const val SERVICE_UUID: String = "12345678-1234-1234-1234-1234567890ab"
    const val COMMAND_UUID: String = "abcdefab-1234-5678-1234-abcdefabcdef"

    const val STATUS_UUID: String = "abcdefac-1234-5678-1234-abcdefabcdef"

    // Estadísticas de Progreso (Modo Aprender)
    var respuestasCorrectas: Int = 0
    var respuestasIncorrectas: Int = 0
    var rachaActual: Int = 0
    var rachaMaxima: Int = 0

    fun registrarAcierto() {
        respuestasCorrectas++
        rachaActual++
        if (rachaActual > rachaMaxima) rachaMaxima = rachaActual
    }

    fun registrarError() {
        respuestasIncorrectas++
        rachaActual = 0
    }
}
