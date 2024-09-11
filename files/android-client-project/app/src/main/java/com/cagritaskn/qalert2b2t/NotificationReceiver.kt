package com.cagritaskn.qalert2b2t

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.util.Log
import androidx.core.content.ContextCompat
import com.cagritaskn.qalert2b2t.R

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            val action = intent.action
            if (action == MainActivity.NOTIFICATION_ACTION_OPEN) {
                // Uygulamayı ön plana getir
                Log.d("NotificationReceiver", "Notification clicked, bringing app to foreground")

                // MainActivity'yi açmak için bir Intent oluştur
                val openAppIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }

                // Uygulamayı başlat
                ContextCompat.startActivity(context, openAppIntent, null)
            }
        }
    }
}
