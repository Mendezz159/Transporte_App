package com.dank.proto_logueo

import com.google.gson.annotations.SerializedName

data class PaisItem(
    @SerializedName("pais")
    val pais: String? = null,

    @SerializedName("codigoIso")
    val codigoIso: String? = null
)

data class CiudadItem(
    @SerializedName("ciudad")
    val ciudad: String? = null,

    @SerializedName("pais")
    val pais: String? = null
)
