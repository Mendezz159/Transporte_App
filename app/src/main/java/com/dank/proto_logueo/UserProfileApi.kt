package com.dank.proto_logueo

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

interface UserProfileApi {

    @GET
    suspend fun getUserProfile(
        @Url url: String,
        @Header("Authorization") authHeader: String
    ): Response<UserProfileResponse>
}
