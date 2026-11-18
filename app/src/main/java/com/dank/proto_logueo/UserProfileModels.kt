package com.dank.proto_logueo

import com.google.gson.annotations.SerializedName

/**
 * Respuesta JSON de la API GET /api/users/{uid}
 */
data class UserProfileResponse(
    @SerializedName("user")
    val user: UserData? = null
)

data class UserData(
    @SerializedName("_id")
    val id: String? = null,

    @SerializedName("uid")
    val uid: String? = null,

    @SerializedName("__v")
    val v: Int? = null,

    @SerializedName("apellido")
    val apellido: String? = null,

    @SerializedName("correo")
    val correo: String? = null,

    @SerializedName("createdAt")
    val createdAt: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("fechaNacimiento")
    val fechaNacimiento: String? = null,

    @SerializedName("nacionalidad")
    val nacionalidad: String? = null,

    @SerializedName("nombre")
    val nombre: String? = null,

    @SerializedName("telefono")
    val telefono: String? = null,

    @SerializedName("updatedAt")
    val updatedAt: String? = null
) {
    val fullName: String
        get() {
            val n = nombre.orEmpty().trim()
            val a = apellido.orEmpty().trim()
            return when {
                n.isNotEmpty() && a.isNotEmpty() -> "$n $a"
                n.isNotEmpty() -> n
                a.isNotEmpty() -> a
                else -> "Usuario Lapticon"
            }
        }

    val displayEmail: String
        get() = email?.takeIf { it.isNotBlank() } ?: correo.orEmpty()
}

/**
 * Body para actualizar perfil de usuario (PUT /api/users/{uid})
 */
data class UpdateUserRequest(
    @SerializedName("nombre")
    val nombre: String? = null,

    @SerializedName("apellido")
    val apellido: String? = null,

    @SerializedName("telefono")
    val telefono: String? = null,

    @SerializedName("nacionalidad")
    val nacionalidad: String? = null
)
