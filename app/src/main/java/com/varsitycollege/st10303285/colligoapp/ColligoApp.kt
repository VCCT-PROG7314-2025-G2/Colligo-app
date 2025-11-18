package com.varsitycollege.st10303285.colligoapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.varsitycollege.st10303285.colligoapp.LocaleHelper

class ColligoApp : Application() {
    override fun onCreate() {
        // apply language early
        LocaleHelper.setLocale(this, LocaleHelper.getSavedLanguage(this))
        super.onCreate()
        createNotificationChannel()
    }

    override fun attachBaseContext(base: android.content.Context) {
        val localeWrapped = LocaleHelper.setLocale(base, LocaleHelper.getSavedLanguage(base))
        super.attachBaseContext(localeWrapped)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "colligo_default"
            val name = "Colligo Notifications"
            val descriptionText = "General updates, ride requests, lost & found"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }

            val notificationManager =
                getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
