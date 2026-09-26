package com.apexus.storagelens.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.apexus.storagelens.MainActivity
import com.apexus.storagelens.R

object Notifications {
    const val CHANNEL_SCAN = "scheduled_scan"
    private const val ID_RECOVERABLE = 1001

    fun createChannels(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_SCAN,
            context.getString(R.string.notification_channel_scan),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.notification_channel_scan_desc) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun showRecoverable(context: Context, formattedSize: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val intent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_SCAN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_recoverable_title, formattedSize))
            .setContentText(context.getString(R.string.notification_recoverable_text))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(ID_RECOVERABLE, notification)
        } catch (e: SecurityException) {
            // Permission de notification révoquée entre-temps : rien à faire.
        }
    }
}
