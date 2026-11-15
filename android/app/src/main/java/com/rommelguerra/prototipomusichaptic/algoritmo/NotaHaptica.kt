package com.rommelguerra.prototipomusichaptic.algoritmo

import org.json.JSONObject

data class NotaHaptica(
    val nota: String,
    val frecuenciaHz: Double,
    val ubicacionEspacial: Double,
    val amplitudSugerida: Int,
    val duracionMs: Long,
    val tiempoInicioS: Double
) {
    /** Database/export representation. BLE uses HapticProtocol, not this JSON. */
    fun toJson(): JSONObject = JSONObject().put("n",nota).put("f",frecuenciaHz).put("u",ubicacionEspacial)
        .put("a",amplitudSugerida).put("d",duracionMs).put("t",tiempoInicioS)
}
