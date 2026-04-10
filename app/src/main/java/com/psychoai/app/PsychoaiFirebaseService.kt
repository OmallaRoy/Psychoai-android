package com.psychoai.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PsychoaiFirebaseService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID_COACHING = "psychoai_coaching"
        const val CHANNEL_ID_DAILY    = "psychoai_daily"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val type     = remoteMessage.data["type"] ?: "coaching"
        val pattern  = remoteMessage.data["pattern"] ?: ""
        val coaching = remoteMessage.data["full_coaching"] ?: ""
        val title    = remoteMessage.notification?.title ?: "Plutus"
        val body     = remoteMessage.notification?.body  ?: ""
        when (type) {
            "coaching" -> showCoachingNotification(title, body, pattern, coaching)
            "daily"    -> showDailyNotification(title, body,
                remoteMessage.data["full_message"] ?: body)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        getSharedPreferences("psychoai_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("fcm_token", token)
            .apply()
    }

    private fun showCoachingNotification(
        title: String, body: String, pattern: String, fullCoaching: String
    ) {
        createChannels()
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("notification_type", "coaching")
            putExtra("pattern", pattern)
            putExtra("coaching", fullCoaching)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_COACHING)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(fullCoaching))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun showDailyNotification(
        title: String, body: String, fullMessage: String
    ) {
        createChannels()
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("notification_type", "daily")
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_DAILY)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(fullMessage))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            NotificationChannel(
                CHANNEL_ID_COACHING,
                "Trading Coaching Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).also { manager.createNotificationChannel(it) }
            NotificationChannel(
                CHANNEL_ID_DAILY,
                "Daily Psychology Insights",
                NotificationManager.IMPORTANCE_DEFAULT
            ).also { manager.createNotificationChannel(it) }
        }
    }
}