package com.dank.proto_logueo

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface FirebaseAuthApi {

    @Headers("Content-Type: application/json")
    @POST("v1/accounts:signInWithPassword")
    suspend fun signInWithPassword(
        @Query("key") apiKey: String,
        @Body request: AuthRequest
    ): Response<AuthResponse>
}
