package com.itant.rt.ui.control

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.itant.rt.R
import com.itant.rt.ui.follow.stream.ScreenManager


class ControlService : Service() {
    companion object {
        const val CHANNEL_ID_STRING = "remote_touch"
        const val ID = 1
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        // 主控端开始连接命令服务器
        MessageSender.startControlService()
    }

    override fun onDestroy() {
        super.onDestroy()
        ScreenManager.recycleExecutorService.submit(Runnable {
            MessageSender.recycleControlService()
        })
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_REDELIVER_INTENT
    }
}

fun Service.startForegroundNotification() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var mChannel: NotificationChannel? = null
        mChannel = NotificationChannel(ControlService.CHANNEL_ID_STRING, getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH)
        mChannel.importance = NotificationManager.IMPORTANCE_LOW
        notificationManager.createNotificationChannel(mChannel)
        val notification: Notification = Notification.Builder(applicationContext,
            ControlService.CHANNEL_ID_STRING
        )
        .setSmallIcon(R.mipmap.ic_notification)
        .setBadgeIconType(NotificationCompat.BADGE_ICON_NONE)
        // 如果有标题的话，桌面会有消息数量提示，但是不设置标题的话，部分手机上要点两次才能进入APP
        //.setContentTitle("")
        //.setContentTitle(getString(R.string.notification_running))
        .build()
        // 点击通知栏打开APP
        notification.flags = Notification.FLAG_ONGOING_EVENT
        startForeground(ControlService.ID, notification)
    }
}