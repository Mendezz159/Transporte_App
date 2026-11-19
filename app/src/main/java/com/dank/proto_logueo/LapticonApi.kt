package com.dank.proto_logueo

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface LapticonApi {

    // --- USUARIOS ---

    /**
     * Crea/Registra el perfil del usuario en el backend de Lapticon (POST /api/users/register)
     */
    @Headers("Content-Type: application/json")
    @POST("api/users/register")
    suspend fun createUserProfile(
        @Header("Authorization") authHeader: String,
        @Body request: CreateUserRequest
    ): Response<UserProfileResponse>

    @GET("api/users/{uid}")
    suspend fun getUserProfile(
        @Path("uid") uid: String,
        @Header("Authorization") authHeader: String
    ): Response<UserProfileResponse>

    @Headers("Content-Type: application/json")
    @PUT("api/users/{uid}")
    suspend fun updateUserProfile(
        @Path("uid") uid: String,
        @Header("Authorization") authHeader: String,
        @Body request: UpdateUserRequest
    ): Response<UserProfileResponse>


    // --- VIAJES ---

    @Headers("Content-Type: application/json")
    @POST("api/viajes")
    suspend fun createViaje(
        @Header("Authorization") authHeader: String,
        @Body request: CreateViajeRequest
    ): Response<Viaje>

    @GET("api/viajes")
    suspend fun getMisViajes(
        @Header("Authorization") authHeader: String
    ): Response<List<Viaje>>

    @GET("api/viajes/disponible")
    suspend fun getViajeDisponible(
        @Header("Authorization") authHeader: String
    ): Response<Viaje>

    @GET("api/viajes/buscar")
    suspend fun buscarViajes(
        @Header("Authorization") authHeader: String,
        @Query("ciudad") ciudad: String
    ): Response<List<Viaje>>


    // --- ENCOMIENDAS ---

    @Headers("Content-Type: application/json")
    @POST("api/encomiendas")
    suspend fun createEncomienda(
        @Header("Authorization") authHeader: String,
        @Body request: CreateEncomiendaRequest
    ): Response<Encomienda>

    @GET("api/encomiendas")
    suspend fun getMisEncomiendas(
        @Header("Authorization") authHeader: String
    ): Response<List<Encomienda>>

    @GET("api/encomiendas/disponibles")
    suspend fun getEncomiendasDisponibles(
        @Header("Authorization") authHeader: String,
        @Query("origen") origen: String,
        @Query("destino") destino: String
    ): Response<List<Encomienda>>

    @Headers("Content-Type: application/json")
    @PUT("api/encomiendas/{id}/estado")
    suspend fun updateEstadoEncomienda(
        @Path("id") id: String,
        @Header("Authorization") authHeader: String,
        @Body request: UpdateEstadoEncomiendaRequest
    ): Response<Encomienda>


    // --- RATINGS ---

    @Headers("Content-Type: application/json")
    @POST("api/ratings")
    suspend fun createRating(
        @Header("Authorization") authHeader: String,
        @Body request: CreateRatingRequest
    ): Response<Rating>

    @GET("api/ratings/{uid}")
    suspend fun getRatingsUsuario(
        @Path("uid") uid: String
    ): Response<RatingListResponse>


    // --- CIUDADES ---

    @GET("api/ciudades/paises")
    suspend fun getPaises(): Response<List<PaisItem>>

    @GET("api/ciudades/ciudades")
    suspend fun getCiudades(): Response<List<CiudadItem>>
}
