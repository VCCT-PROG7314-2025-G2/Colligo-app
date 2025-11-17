package com.varsitycollege.st10303285.colligoapp.network

import retrofit2.http.*
import retrofit2.Response

// Contains all functions that talk to the backend.
// Each function represents an API endpoint.


interface ApiService {

    // -------------------------------------------- RIDES ---------------------------------------------------------------
    @POST("rides")
    suspend fun createRide(@Body body: Map<String, Any>): Response<Map<String, Any>>

    @GET("rides")
    suspend fun getRides(@Query("status") status: String = "open"): Response<List<Map<String, Any>>>

    @POST("rides/{rideId}/requests")
    suspend fun sendRideRequest(
        @Path("rideId") rideId: String,
        @Body body: Map<String, Any>
    ): Response<Map<String, Any>>

    @POST("rides/{rideId}/requests/{requestId}/accept")
    suspend fun acceptRequest(
        @Path("rideId") rideId: String,
        @Path("requestId") requestId: String
    ): Response<Map<String, Any>>


    // ------------------------------------------- LOST & FOUND ----------------------------------------------------------
    @POST("lost")
    suspend fun createLostItem(@Body body: Map<String, Any>): Response<Map<String, Any>>

    @GET("lost")
    suspend fun getLostItems(): Response<List<Map<String, Any>>>


    // --------------------------------------------- TIMETABLE ---------------------------------------------------------------
    @POST("timetable/{uid}/events")
    suspend fun createEvent(
        @Path("uid") uid: String,
        @Body body: Map<String, Any>
    ): Response<Map<String, Any>>

    @GET("timetable/{uid}/events")
    suspend fun getEvents(@Path("uid") uid: String): Response<List<Map<String, Any>>>


    // ------------------------------------------ MAP LOCATIONS ----------------------------------------------------------------
    @GET("map/locations")
    suspend fun getLocations(): Response<List<Map<String, Any>>>

    @POST("map/locations")
    suspend fun addLocation(@Body body: Map<String, Any>): Response<Map<String, Any>>
}
