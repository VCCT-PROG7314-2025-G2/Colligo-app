package com.varsitycollege.st10303285.colligoapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RidesDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rides_dashboard)

        // Left side: OFFER A RIDE
        val areaOffer: View = findViewById(R.id.areaOffer)
        val tvOffer: TextView = findViewById(R.id.tvOfferRide)

        // Right side: REQUEST A RIDE
        val areaRequest: View = findViewById(R.id.areaRequest)
        val tvRequest: TextView = findViewById(R.id.tvRequestRide)

        val goToOffer = View.OnClickListener {
            startActivity(Intent(this, OfferRideActivity::class.java))
        }
        val goToRequest = View.OnClickListener {
            startActivity(Intent(this, RequestRideSearchActivity::class.java))
        }

        // Make both the coloured area and the text clickable
        areaOffer.setOnClickListener(goToOffer)
        tvOffer.setOnClickListener(goToOffer)

        areaRequest.setOnClickListener(goToRequest)
        tvRequest.setOnClickListener(goToRequest)

        // (Optional) hook up top-bar icons or bottom nav here if you want
    }
}
