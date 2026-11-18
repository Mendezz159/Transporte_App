package com.dank.proto_logueo

import com.google.gson.annotations.SerializedName

/**
 * Modelo para representación de Viaje/Vuelo en Lapticon
 */
data class Viaje(
    @SerializedName("_id")
    val id: String? = null,

    @SerializedName("usuarioId")
    val usuarioId: String? = null,

    @SerializedName("numeroVuelo")
    val numeroVuelo: String? = null,

    @SerializedName("ciudadOrigen")
    val ciudadOrigen: String? = null,

    @SerializedName("ciudadDestino")
    val ciudadDestino: String? = null,

    @SerializedName("fechaViaje")
    val fechaViaje: String? = null,

    @SerializedName("estado")
    val estado: String? = null,

    @SerializedName("createdAt")
    val createdAt: String? = null,

    @SerializedName("updatedAt")
    val updatedAt: String? = null
)

/**
 * Request para crear un viaje (POST /api/viajes)
 */
data class CreateViajeRequest(
    @SerializedName("numeroVuelo")
    val numeroVuelo: String,

    @SerializedName("ciudadOrigen")
    val ciudadOrigen: String,

    @SerializedName("ciudadDestino")
    val ciudadDestino: String,

    @SerializedName("fechaViaje")
    val fechaViaje: String
)
