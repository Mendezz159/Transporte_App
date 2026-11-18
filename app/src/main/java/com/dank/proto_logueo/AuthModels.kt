package com.dank.proto_logueo

import com.google.gson.annotations.SerializedName

/**
 * Request body para https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword
 */
data class AuthRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("returnSecureToken")
    val returnSecureToken: Boolean = true
)

/**
 * Response exitosa (HTTP 200) de Firebase Identity Toolkit API
 */
data class AuthResponse(
    @SerializedName("kind")
    val kind: String? = null,

    @SerializedName("localId")
    val localId: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("displayName")
    val displayName: String? = null,

    @SerializedName("idToken")
    val idToken: String? = null,

    @SerializedName("registered")
    val registered: Boolean? = null,

    @SerializedName("refreshToken")
    val refreshToken: String? = null,

    @SerializedName("expiresIn")
    val expiresIn: String? = null
)

/**
 * Respuesta de error de Firebase API (HTTP 400 / 403 / 500)
 */
data class AuthErrorResponse(
    @SerializedName("error")
    val error: AuthErrorDetails? = null
)

data class AuthErrorDetails(
    @SerializedName("code")
    val code: Int? = null,

    @SerializedName("message")
    val message: String? = null
)
