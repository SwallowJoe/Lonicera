package com.android.mcpsdk.server

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.android.mcpsdk.IPipeService
import com.android.mcpsdk.R

class PipeService : Service() {
    companion object {
        private const val TAG = "PipeService"
        const val ACTION_PIPE_SERVICE = "android.intent.action.PipeService"

        @SuppressLint("ObsoleteSdkInt")
        fun startForegroundServiceIfNeeded(context: Context, connection: ServiceConnection) {
            Log.i(TAG, "startForegroundServiceIfNeeded")
            val intent = Intent(
                context.applicationContext,
                PipeService::class.java
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.applicationContext.startForegroundService(intent)
            } else {
                context.applicationContext.startService(intent)
            }

            context.applicationContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        fun stopService(context: Context) {
            context.applicationContext.stopService(
                Intent(
                    context.applicationContext,
                    PipeService::class.java
                )
            )
        }

    }
    private val binder = object : IPipeService.Stub() {
        override fun openPipe(name: String?): ParcelFileDescriptor? {
            if (name == null)  return null
            val fds = ParcelFileDescriptor.createSocketPair()
            BinderMcpServer.newServer(name, fds[0])
            return fds[1]
        }
    }

    override fun onBind(intent: Intent): IBinder = binder
    override fun onCreate() {
        super.onCreate()
        showForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun showForegroundNotification() {
        val notification = createNotification()
        startForeground(1, notification)
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun createNotification(): Notification {
        val channelId = "pipe_channel"
        // 创建通知渠道（Android 8.0 及以上需要）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "PipeService",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("PipeService Running")
            .setContentText("Performing pipe service for mcp server...")
            .setSmallIcon(R.drawable.support_48px)
            .build()
    }

}