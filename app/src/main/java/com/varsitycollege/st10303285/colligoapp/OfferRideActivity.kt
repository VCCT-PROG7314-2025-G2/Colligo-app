package com.varsitycollege.st10303285.colligoapp

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.varsitycollege.st10303285.colligoapp.OfferRideViewModel
import java.time.Instant
import java.util.*

class OfferRideActivity : AppCompatActivity() {

    private lateinit var vm: OfferRideViewModel
    private lateinit var timeText: TextView
    private var selectedTimeIso = ""
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offer_ride)

        vm = ViewModelProvider(this)[OfferRideViewModel::class.java]

        val etStart = findViewById<EditText>(R.id.etStart)
        val etDest = findViewById<EditText>(R.id.etDestination)
        val etSeats = findViewById<EditText>(R.id.etSeats)
        val etNotes = findViewById<EditText>(R.id.etNotes)
        timeText = findViewById(R.id.tvTime)


        progress = ProgressBar(this, null, android.R.attr.progressBarStyleLarge)
        progress.visibility = android.view.View.GONE
        (findViewById<ViewGroup>(android.R.id.content)).addView(progress)

        val btnPickTime = findViewById<Button>(R.id.btnPickTime)
        btnPickTime.setOnClickListener { showTimePicker() }

        findViewById<Button>(R.id.btnViewMyRides).setOnClickListener {
            startActivity(Intent(this, MyRidesActivity::class.java))
        }

        val btnPost = findViewById<Button>(R.id.btnPostRide)
        btnPost.setOnClickListener {
            val start = etStart.text.toString().trim()
            val dest = etDest.text.toString().trim()
            if (start.isEmpty() || dest.isEmpty() || selectedTimeIso.isEmpty()) {
                Toast.makeText(this, "Please fill start, destination and time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val body = mapOf(
                "start" to start,
                "destination" to dest,
                "dateTime" to selectedTimeIso,
                "seatsTotal" to (etSeats.text.toString().toIntOrNull() ?: 1),
                "notes" to etNotes.text.toString()
            )

            progress.visibility = android.view.View.VISIBLE
            btnPost.isEnabled = false

            vm.postRide(body) { success, payload ->
                runOnUiThread {
                    progress.visibility = android.view.View.GONE
                    btnPost.isEnabled = true
                    if (success) {
                        Log.d("OfferRide", "Ride posted successfully id=$payload")
                        Toast.makeText(this, "Ride posted", Toast.LENGTH_SHORT).show()

                        val i = Intent(this, OfferRideSuccessActivity::class.java)
                        i.putExtra("rideId", payload)
                        startActivity(i)
                        finish()
                    } else {
                        Log.e("OfferRide", "Post failed: $payload")
                        Toast.makeText(this, "Failed to post ride: $payload", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun showTimePicker() {
        val cal = Calendar.getInstance()
        TimePickerDialog(this, { _, hour, minute ->
            val now = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            now.set(Calendar.HOUR_OF_DAY, hour)
            now.set(Calendar.MINUTE, minute)
            now.set(Calendar.SECOND, 0)
            now.set(Calendar.MILLISECOND, 0)
            selectedTimeIso = Instant.ofEpochMilli(now.timeInMillis).toString()
            timeText.text = String.format(Locale.getDefault(), "%02d:%02d (UTC)", hour, minute)
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }
}
