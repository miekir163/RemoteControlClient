package com.itant.rt.ui.follow.stream

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import com.itant.rt.ui.control.startForegroundNotification
import com.itant.rt.ui.follow.MessageReceiver
import com.itant.rt.utils.AliveManager


class ScreenService : Service() {
    companion object {
        private const val CHANNEL_ID_STRING = "remote_touch"
        private const val ID = 1
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private var wakeLock: WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        // 受控端启动录屏线程
        startCaptureThread()
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "rt:wake_internet")
        wakeLock?.run {
            if (!isHeld) { acquire() }
        }
        AliveManager.start()
    }

    private fun startCaptureThread() {
        if (ScreenManager.threadHandler != null) {
            return
        }
        val childThread = object : Thread() {
            override fun run() {
                //android.os.Process.setThreadPriority(THREAD_PRIORITY_FOREGROUND)
                Looper.prepare()
                ScreenManager.threadHandler = Handler(Looper.myLooper()!!)
                // 受控端开启推流服务
                ScreenManager.startPublishStreamService()
                // 受控端开始监听指令
                MessageReceiver.startFollowService()
                Looper.loop()
            }
        }
        childThread.setPriority(Thread.MAX_PRIORITY)
        childThread.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeLock?.run {
            if (!isHeld) {
                release()
            }
        }
        ScreenManager.recycleExecutorService.submit(Runnable {
            MessageReceiver.recycleFollowService()
            ScreenManager.recyclePublishStreamService()
        })

        ScreenManager.threadHandler?.looper?.quit()
        ScreenManager.threadHandler = null

        ScreenManager.mProjectionIntent = null
        ScreenManager.mediaProjection?.stop()
        ScreenManager.mediaProjection = null

        ScreenManager.lastBitmap?.recycle()
        ScreenManager.lastBitmap = null

        ScreenManager.recycleCapture()

        AliveManager.stop()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_REDELIVER_INTENT
    }
}