package com.dank.proto_logueo

import com.google.gson.annotations.SerializedName

/**
 * Modelo para encomiendas (envíos estilo Uber Connect)
 */
data class Encomienda(
    @SerializedName("_id")
    val id: String? = null,

    @SerializedName("usuarioId")
    val usuarioId: String? = null,

    @SerializedName("ciudadOrigen")
    val ciudadOrigen: String? = null,

    @SerializedName("ciudadDestino")
    val ciudadDestino: String? = null,

    @SerializedName("observaciones")
    val observaciones: String? = null,

    @SerializedName("moneda")
    val moneda: String? = null,

    @SerializedName("valor")
    val valor: Double? = null,

    @SerializedName("estado")
    val estado: String? = null,

    @SerializedName("createdAt")
    val createdAt: String? = null,

    @SerializedName("updatedAt")
    val updatedAt: String? = null
)

/**
 * Request para crear encomienda (POST /api/encomiendas)
 */
data class CreateEncomiendaRequest(
    @SerializedName("ciudadOrigen")
    val ciudadOrigen: String,

    @SerializedName("ciudadDestino")
    val ciudadDestino: String,

    @SerializedName("observaciones")
    val observaciones: String,

    @SerializedName("moneda")
    val moneda: String = "USD",

    @SerializedName("valor")
    val valor: Double
)

/**
 * Request para actualizar estado de encomienda (PUT /api/encomiendas/{id}/estado)
 */
data class UpdateEstadoEncomiendaRequest(
    @SerializedName("estado")
    val estado: String
)
