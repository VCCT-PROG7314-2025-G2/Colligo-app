package com.varsitycollege.st10303285.colligoapp

import android.os.Bundle
import android.content.Intent
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import android.widget.Button
import com.varsitycollege.st10303285.colligoapp.R

class SplashActivity : AppCompatActivity() {
    // simple splash that checks if user is already signed in
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val btn: Button = findViewById(R.id.btnGetStarted)
        btn.setOnClickListener {
            goNext()
        }


        Handler(mainLooper).postDelayed({ goNext() }, 1800)
    }

    private fun goNext() {
        val user = Firebase.auth.currentUser
        if (user != null) {
            // user signed in -> go to main screen
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // not signed in -> go to login
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }
}