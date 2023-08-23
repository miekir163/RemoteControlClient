package com.itant.rt.utils

import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.PendingIntent
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.text.TextUtils
import androidx.legacy.content.WakefulBroadcastReceiver
import com.blankj.utilcode.util.NetworkUtils
import com.itant.rt.ui.follow.FollowManager
import com.miekir.mvp.common.context.GlobalContext
import com.miekir.mvp.common.log.L
import java.util.concurrent.Executors


/**
 * 定时检查是否已经没有网络了，没有网络则亮屏唤醒一下
 */
object AliveManager {
    private const val ACTION_STOP_SCAN = "keep_alive_rest"

    /**
     * 30秒检查一下
     */
    private const val CHECK_INTERVAL_MILLIS = 5 * 60_000L

    private val checkNetExecutor = Executors.newSingleThreadExecutor()
    private val mAlarmManager = GlobalContext.getContext().getSystemService(ALARM_SERVICE) as AlarmManager

    private val taskReceiver = object : WakefulBroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            checkConnectServerStatus()
        }
    }

    private fun checkConnectServerStatus() {
        pinging = true
        val serverIp = FollowManager.currentFollowBean.p
        if (!TextUtils.isEmpty(serverIp)) {
            checkNetExecutor.submit(Runnable {
                val networkAvailable = NetworkUtils.isAvailableByPing(serverIp)
                // 到服务器的网络不可达，那么就亮屏唤起CPU
                if (!networkAvailable && !stopped) {
                    AppUtils.wakeupScreen()
                }
                GlobalContext.runOnUiThread {
                    // 继续下一次扫描
                    pinging = false
                    if (!stopped) {
                        start()
                    }
                }
            })
        } else {
            // 继续下一次扫描
            pinging = false
            if (!stopped) {
                start()
            }
        }
    }

    private val restIntent = Intent(ACTION_STOP_SCAN)
    private val restPendingIntent = PendingIntent.getBroadcast(
        GlobalContext.getContext(),
        0,
        restIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    private val intentFilter = IntentFilter().apply {
        addAction(ACTION_STOP_SCAN)
    }

    @Volatile
    private var stopped = true
    @Volatile
    private var pinging = false
    /**
     * 开始定时检查
     */
    fun start() {
        if (pinging) {
            return
        }
        stopped = false
        mAlarmManager.cancel(restPendingIntent)
        GlobalContext.getContext().registerReceiver(taskReceiver, intentFilter, null, null)
        val alarmClockInfo = AlarmClockInfo(System.currentTimeMillis() + CHECK_INTERVAL_MILLIS, null)
        mAlarmManager.setAlarmClock(alarmClockInfo, restPendingIntent)
    }

    fun stop() {
        stopped = true
        mAlarmManager.cancel(restPendingIntent)
        try {
            GlobalContext.getContext().unregisterReceiver(taskReceiver)
        } catch (e: Exception) {
            L.e(e.message)
        }
    }
}