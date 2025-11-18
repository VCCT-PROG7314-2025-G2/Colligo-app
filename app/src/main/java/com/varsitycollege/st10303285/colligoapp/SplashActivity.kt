package com.varsitycollege.st10303285.colligoapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DELAY: Long = 1800 // 1.8 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        setupClickListeners()
        scheduleNavigation()
    }

    private fun setupClickListeners() {
        val btnGetStarted: Button = findViewById(R.id.btnGetStarted)
        btnGetStarted.setOnClickListener {
            // Remove any pending delayed navigation when button is clicked
            Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null)
            goNext()
        }
    }

    private fun scheduleNavigation() {
        Handler(Looper.getMainLooper()).postDelayed({
            goNext()
        }, SPLASH_DELAY)
    }

    private fun goNext() {
        val user = Firebase.auth.currentUser
        val destination = if (user != null) {
            // User is signed in -> go to main screen
            HomeActivity::class.java
        } else {
            // User not signed in -> go to login
            LoginActivity::class.java
        }

        startActivity(Intent(this, destination))
        finish()
    }
}