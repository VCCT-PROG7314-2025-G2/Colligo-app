package com.varsitycollege.st10303285.colligoapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.varsitycollege.st10303285.colligoapp.repository.ApiRepository
import kotlinx.coroutines.launch

class RequestRideActivity : AppCompatActivity() {

    private val repo = ApiRepository()
    private var rideId: String? = null
    private lateinit var tvInfo: TextView
    private lateinit var btnRequest: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_ride_results) // reuse; shows list area

        rideId = intent.getStringExtra("rideId")

        // simple programmatic header + button added above the RecyclerView
        val container = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvRides).parent as? android.view.ViewGroup
        tvInfo = TextView(this).apply { textSize = 16f; setPadding(12,12,12,12) }
        btnRequest = Button(this).apply { text = "Request Seat/ View details" }
        container?.addView(tvInfo, 0)
        container?.addView(btnRequest, 1)

        if (rideId == null) {
            Toast.makeText(this, "No ride specified", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadRideDetails()

        btnRequest.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val resp = repo.requestSeat(rideId!!, mapOf("seatsRequested" to 1, "message" to "I'd like a seat"))
                    if (resp.isSuccessful) {
                        Toast.makeText(this@RequestRideActivity, "Request sent", Toast.LENGTH_SHORT).show()
                        startActivity(android.content.Intent(this@RequestRideActivity, RequestSentActivity::class.java))
                    } else {
                        Toast.makeText(this@RequestRideActivity, "Failed to send request", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@RequestRideActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadRideDetails() {
        lifecycleScope.launch {
            try {
                val resp = repo.getRideDetails(rideId!!)
                if (resp.isSuccessful) {
                    val ride = resp.body() ?: emptyMap<String, Any>()
                    val driver = ride["driverId"]?.toString() ?: "Unknown"
                    val start = ride["start"]?.toString() ?: ""
                    val dest = ride["destination"]?.toString() ?: ""
                    val time = ride["dateTime"]?.toString() ?: "N/A"
                    tvInfo.text = "From: $start\nTo: $dest\nTime: $time\nDriver: $driver"
                } else {
                    tvInfo.text = "Failed to load ride details"
                }
            } catch (e: Exception) {
                tvInfo.text = "Error: ${e.message}"
            }
        }
    }
}
