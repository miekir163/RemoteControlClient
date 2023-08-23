package com.itant.rt.ui.control

import android.content.Intent
import android.provider.Settings.Global
import android.text.TextUtils
import androidx.core.content.ContextCompat
import com.itant.rt.R
import com.itant.rt.RtConstants.MESSAGE_SPLIT
import com.itant.rt.ext.enableCommonChat
import com.itant.rt.storage.KeyValue
import com.itant.rt.ui.follow.FollowManager
import com.itant.rt.ui.follow.action.ActionManager
import com.itant.rt.ui.follow.action.ActionType
import com.itant.rt.utils.AppUtils
import com.miekir.mvp.common.context.GlobalContext
import com.miekir.mvp.common.log.L
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.util.concurrent.Executors

/**
 * 放到Service里开始结束
 */
object MessageSender {
    private val controlExecutorService = Executors.newSingleThreadExecutor()

    @Volatile
    private var controlSocket: ZMQ.Socket? = null

    @Volatile
    private var controlContext: ZContext? = null

    @Volatile
    private var controlCmdUrl = ""

    /**
     * 主控端连接服务器
     */
    fun startControlService() {
        controlCmdUrl = "tcp://${FollowManager.currentFollowBean.p}:${FollowManager.currentFollowBean.p3}"
        controlExecutorService.submit(Runnable {
            if (controlSocket != null) {
                return@Runnable
            }

            try {
                controlContext = ZContext()
                controlSocket = controlContext?.createSocket(SocketType.PUB)
                controlSocket?.enableCommonChat()
                controlSocket?.connect(controlCmdUrl)

                // 点击受控端列表item时，会先发送两个指令过去（让受控端开始推流），先等待几秒再发送，因为建立连接需要时间，太早发送会失败
                Thread.sleep(3500)
                sendCmd(ActionType.CLICK, 0.0f, 0.0f, 0.0f, 1.0f, ActionManager.TIME_START)
            } catch (e: Exception) {
                AppUtils.killSelf(GlobalContext.getContext().getString(R.string.control_start_failed))
            }
        })
    }

    fun startControl() {
        GlobalContext.getContext().run {
            val controllerIntent = Intent(this, ControlService::class.java)
            ContextCompat.startForegroundService(this, controllerIntent)
        }
    }

    /**
     * 发送指令给受控端，每一个参数都不能为空
     * 命令格式：收信人ID~动作类型（短击，长按，滑动）~startX~startY~endX~endY~发送时间戳（这里的浮点数是屏幕比例不是像素）
     */
    fun sendCmd(
        touchType: Int,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        duration: Long
    ) {
        val deviceId = FollowManager.currentFollowBean.i
        if (TextUtils.isEmpty(deviceId)) {
            return
        }
        controlExecutorService.submit(Runnable {
            try {
                controlSocket?.send("${deviceId}${MESSAGE_SPLIT}${touchType}${MESSAGE_SPLIT}${startX}${MESSAGE_SPLIT}${startY}${MESSAGE_SPLIT}${endX}${MESSAGE_SPLIT}${endY}${MESSAGE_SPLIT}${duration}")
            } catch (e: Exception) {
                L.e(e.message)
            }
        })
    }

    fun sendCmdToExplicitDevice(
        touchType: Int,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        duration: Long,
        deviceId: String
    ) {
        if (TextUtils.isEmpty(deviceId)) {
            return
        }
        controlExecutorService.submit(Runnable {
            try {
                controlSocket?.send("${deviceId}${MESSAGE_SPLIT}${touchType}${MESSAGE_SPLIT}${startX}${MESSAGE_SPLIT}${startY}${MESSAGE_SPLIT}${endX}${MESSAGE_SPLIT}${endY}${MESSAGE_SPLIT}${duration}")
            } catch (e: Exception) {
                L.e(e.message)
            }
        })
    }



    fun stopControl() {
        GlobalContext.getContext().run {
            val controllerIntent = Intent(this, ControlService::class.java)
            stopService(controllerIntent)
        }
    }

    fun recycleControlService() {
        try {
            controlSocket?.disconnect(controlCmdUrl)
        } catch (e: Exception) {
            L.e(e.message)
        }

        try {
            controlContext?.destroy()
        } catch (e: Exception) {
            L.e(e.message)
        }

        controlContext = null
        controlSocket = null
    }
}