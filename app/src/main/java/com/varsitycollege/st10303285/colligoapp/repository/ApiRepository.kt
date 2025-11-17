package com.varsitycollege.st10303285.colligoapp.repository

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.varsitycollege.st10303285.colligoapp.network.ApiService
import com.varsitycollege.st10303285.colligoapp.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// This class acts as a bridge between the ViewModels and the ApiService.
// It contains functions that call the ApiService methods and handle any necessary data processing.


class ApiRepository {
    private val api = RetrofitClient.instance.create(ApiService::class.java)

    private fun getUid(): String? = Firebase.auth.currentUser?.uid

    // -------------------------------------------- RIDES --------------------------------------------------------
    suspend fun getRides() = withContext(Dispatchers.IO) {
        api.getRides()
    }

    suspend fun createRide(body: Map<String, Any>) = withContext(Dispatchers.IO) {
        api.createRide(body)
    }

    suspend fun requestSeat(rideId: String, body: Map<String, Any>) = withContext(Dispatchers.IO) {
        api.sendRideRequest(rideId, body)
    }

    suspend fun acceptRequest(rideId: String, requestId: String) = withContext(Dispatchers.IO) {
        api.acceptRequest(rideId, requestId)
    }


    // ----------------------------------------- LOST ----------------------------------------------------------------
    suspend fun createLostItem(body: Map<String, Any>) = withContext(Dispatchers.IO) {
        api.createLostItem(body)
    }

    suspend fun getLostItems() = withContext(Dispatchers.IO) {
        api.getLostItems()
    }


    // --------------------------------------- TIMETABLE ------------------------------------------------------------
    suspend fun createEvent(body: Map<String, Any>) = withContext(Dispatchers.IO) {
        val uid = getUid() ?: return@withContext null
        api.createEvent(uid, body)
    }

    suspend fun getEvents() = withContext(Dispatchers.IO) {
        val uid = getUid() ?: return@withContext null
        api.getEvents(uid)
    }

    // -------------------------------------- MAP --------------------------------------------------------------------
    suspend fun getLocations() = withContext(Dispatchers.IO) {
        api.getLocations()
    }

    suspend fun addLocation(body: Map<String, Any>) = withContext(Dispatchers.IO) {
        api.addLocation(body)
    }
}