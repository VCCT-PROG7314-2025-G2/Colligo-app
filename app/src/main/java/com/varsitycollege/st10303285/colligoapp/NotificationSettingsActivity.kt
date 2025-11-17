package com.varsitycollege.st10303285.colligoapp

import android.content.Context
import android.os.Bundle
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.messaging.FirebaseMessaging

class NotificationSettingsActivity : AppCompatActivity() {

    private lateinit var swPush: Switch
    private val PREFS = "app_prefs"
    private val KEY_PUSH = "pref_push"
    private val TOPIC = "global_updates"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_settings)

        swPush = findViewById(R.id.switchPush)
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_PUSH, true)
        swPush.isChecked = enabled

        swPush.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            prefs.edit().putBoolean(KEY_PUSH, isChecked).apply()
            if (isChecked) subscribeToTopic() else unsubscribeFromTopic()
        }
    }

    private fun subscribeToTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, getString(R.string.notifications_enabled), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getString(R.string.notifications_failed), Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun unsubscribeFromTopic() {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(TOPIC)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, getString(R.string.notifications_disabled), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getString(R.string.notifications_failed), Toast.LENGTH_SHORT).show()
                }
            }
    }
}