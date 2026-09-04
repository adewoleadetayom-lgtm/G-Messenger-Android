package com.gmessenger.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class GmFirebaseMessagingService : FirebaseMessagingService() {
    companion object { const val CHANNEL_ID = "gm_messages" }
    override fun onNewToken(token: String) {
        getSharedPreferences("gm_push", MODE_PRIVATE).edit().putString("token", token).apply()
    }
    override fun onMessageReceived(message: RemoteMessage) {
        val data=message.data
        val title=data["title"] ?: message.notification?.title ?: "G Messenger"
        val body=data["body"] ?: message.notification?.body ?: "New message"
        showNotification(title, body)
    }
    private fun showNotification(title:String, body:String){
        val nm=getSystemService(NotificationManager::class.java)
        if(Build.VERSION.SDK_INT>=26) nm.createNotificationChannel(NotificationChannel(CHANNEL_ID,"Messages",NotificationManager.IMPORTANCE_HIGH))
        val intent=Intent(this,MainActivity::class.java).apply{flags=Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP}
        val pi=PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n=NotificationCompat.Builder(this,CHANNEL_ID)
            .setSmallIcon(com.gmessenger.app.R.drawable.ic_gm_logo)
            .setContentTitle(title).setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).setContentIntent(pi).build()
        nm.notify((System.currentTimeMillis()%100000).toInt(),n)
    }
}
