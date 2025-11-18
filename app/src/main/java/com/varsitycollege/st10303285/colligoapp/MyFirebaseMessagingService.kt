package com.varsitycollege.st10303285.colligoapp

import android.R.id.title
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val TAG = "FCM-SERVICE"
    private val CHANNEL_ID = "colligo-notifs"

    override fun onCreate() {
        super.onCreate()
        createChannelIfNeeded()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "onNewToken: $token")

    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "onMessageReceived - from: ${message.from}")
        if (message.data.isNotEmpty()) {
            Log.d(TAG, "onMessageReceived - data: ${message.data}")
        }
        message.notification?.let {
            Log.d(TAG, "onMessageReceived - notification: title=${it.title} body=${it.body}")
        }

        val title = message.notification?.title ?: message.data["title"] ?: "Colligo"
        val body = message.notification?.body ?: message.data["body"] ?: "You have a new event"

        // 👇 Decide where to go when tapping the notification
        val targetIntent = if (message.data["rideId"] != null) {
            // New ride request → go to DriverRequestsActivity
            Intent(this, DriverRequestsActivity::class.java).apply {
                putExtra("rideId", message.data["rideId"])
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            // Fallback: just go home
            Intent(this, HomeActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        val pending = PendingIntent.getActivity(
            this, 0, targetIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(pending)
            .build()

        nm.notify((System.currentTimeMillis() % 10000).toInt(), notif)
    }


    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Colligo notifications"
            val desc = "Channel for Colligo push notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = desc
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }
}
