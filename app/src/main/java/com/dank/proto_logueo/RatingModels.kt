package com.dank.proto_logueo

import com.google.gson.annotations.SerializedName

/**
 * Modelo de Calificación / Rating
 */
data class Rating(
    @SerializedName("_id")
    val id: String? = null,

    @SerializedName("usuarioOrigen")
    val usuarioOrigen: String? = null,

    @SerializedName("usuarioDestino")
    val usuarioDestino: String? = null,

    @SerializedName("rolDestino")
    val rolDestino: String? = null,

    @SerializedName("calificacion")
    val calificacion: Int? = null,

    @SerializedName("comentario")
    val comentario: String? = null,

    @SerializedName("createdAt")
    val createdAt: String? = null
)

/**
 * Respuesta JSON del endpoint GET /api/ratings/{uid}
 */
data class RatingListResponse(
    @SerializedName("total")
    val total: Int? = 0,

    @SerializedName("promedio")
    val promedio: Double? = null,

    @SerializedName("calificaciones")
    val calificaciones: List<Rating>? = emptyList()
)

/**
 * Request para crear calificación (POST /api/ratings)
 */
data class CreateRatingRequest(
    @SerializedName("usuarioDestino")
    val usuarioDestino: String,

    @SerializedName("rolDestino")
    val rolDestino: String = "viajero",

    @SerializedName("calificacion")
    val calificacion: Int,

    @SerializedName("comentario")
    val comentario: String
)
