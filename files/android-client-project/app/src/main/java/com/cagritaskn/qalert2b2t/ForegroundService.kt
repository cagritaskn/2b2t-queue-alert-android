package com.cagritaskn.qalert2b2t

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class ForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "ForegroundServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()
        // Foreground Service başlatıldığında yapılacak işlemler
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Foreground Service")
            .setContentText("The 2B2T Queue Alert app is running in the background to receive queue data and send notifications.")
            .setSmallIcon(R.drawable.notification_icon)
            .build()

        startForeground(1, notification)

        // Uygulamanın verileri çekmeye devam etmesi için işlemlere devam edin
        // Örneğin, verileri getiren Runnable'ınızı buradan tetikleyebilirsiniz

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // Service durdurulduğunda yapılacak işlemler
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_LOW // Ses ve titreşim olmadan bildirim
            ).apply {
                setSound(null, null) // Kanalın sesini kapalı yapar
            }
            val manager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
