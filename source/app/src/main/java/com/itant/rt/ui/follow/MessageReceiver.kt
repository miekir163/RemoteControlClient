package com.itant.rt.ui.follow

import android.text.TextUtils
import com.itant.rt.R
import com.itant.rt.RtConstants
import com.itant.rt.ext.enableCommonChat
import com.itant.rt.storage.KeyValue
import com.itant.rt.ui.follow.action.ActionManager
import com.itant.rt.ui.follow.action.ActionType
import com.itant.rt.ui.follow.stream.ScreenManager
import com.itant.rt.utils.AppUtils
import com.miekir.mvp.common.context.GlobalContext
import com.miekir.mvp.common.log.L
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.util.concurrent.Executors

object MessageReceiver {
    /**
     * 1分钟内没有收到过指令，就不要推流了，节省流量
     */
    private const val STREAM_IDLE_MILLIS = 60_000L

    private val followExecutorService = Executors.newSingleThreadExecutor()
    private val actionExecutorService = Executors.newSingleThreadExecutor()

    @Volatile
    private var followSocket: ZMQ.Socket? = null

    @Volatile
    private var followCmdUrl = ""

    @Volatile
    private var followContext: ZContext? = null

    /**
     * 上一次发送指令给当前设备的时间戳
     */
    @Volatile
    var lastCmdMillis: Long = 0L

    /**
     * 受控端连接服务器
     */
    fun startFollowService() {
        followCmdUrl = "tcp://${FollowManager.currentFollowBean.p}:${FollowManager.currentFollowBean.p4}"
        followExecutorService.submit(Runnable {
            if (followSocket != null) {
                return@Runnable
            }
            try {
                followContext = ZContext()
                followSocket = followContext?.createSocket(SocketType.SUB)
                followSocket?.enableCommonChat()
                followSocket?.connect(followCmdUrl)
                followSocket?.subscribe("".toByteArray())
            } catch (e: Exception) {
                AppUtils.killSelf(GlobalContext.getContext().getString(R.string.follow_command_failed))
            }

            while (followSocket != null) {
                val receivedData = followSocket?.recv(0)
                // 当前不是受控端的话，不用处理指令
                if (receivedData == null || receivedData.isEmpty()) {
                    continue
                }

                val cmdString = String(receivedData, ZMQ.CHARSET)
                val cmdArray = cmdString.split(RtConstants.MESSAGE_SPLIT)
                // 非法指令，不处理
                if (cmdArray.size != RtConstants.CMD_SIZE) {
                    continue
                }

                // 指令不是发送给当前设备的
                if (!TextUtils.equals(cmdArray[0], KeyValue.deviceId)) {
                    lastCmdMillis = 0L
                    continue
                }

                actionExecutorService.submit(Runnable {
                    // 解析命令
                    parseCmd(cmdArray)
                })
            }
        })
    }

    private fun parseCmd(cmdArray: List<String>) {
        // 没有无障碍权限
        if (!ScreenManager.accessibilityEnabled) {
            return
        }

        // 收信人ID~动作类型（短击，长按，滑动）~startX~startY~endX~endY~动作时长（这里的浮点数是屏幕比例不是像素）
        val touchType = cmdArray[1].toInt()
        val startX = cmdArray[2].toFloat()
        val startY = cmdArray[3].toFloat()
        val endX = cmdArray[4].toFloat()
        val endY = cmdArray[5].toFloat()
        val duration = cmdArray[6].toLong()
        lastCmdMillis = if (touchType == ActionType.CONTROL_LEAVE) {
            ActionManager.lockScreen()
            0L
        } else {
            ActionManager.wakeupScreenIfNeed()
            System.currentTimeMillis()
        }
        when (touchType) {
            ActionType.CLICK -> {
                ActionManager.click(endX, endY)
            }
            ActionType.LONG_CLICK -> {
                ActionManager.longClick(endX, endY)
            }
            ActionType.SWIPE -> {
                ActionManager.swipe(startX, startY, endX, endY, duration)
            }
            ActionType.DESKTOP -> {
                ActionManager.goDesktop()
            }
            ActionType.BACK -> {
                ActionManager.goBack()
            }
            ActionType.OPTIONS -> {
                ActionManager.openRecent()
            }
            ActionType.VIDEO_QUALITY -> {
                KeyValue.videoQuality = duration.toInt()
            }
            ActionType.VIDEO_SIZE -> {
                KeyValue.videoMaxSize = duration.toInt()
            }
            else -> {
                L.e("未知动作")
            }
        }
    }

    fun recycleFollowService() {
        try {
            followSocket?.disconnect(followCmdUrl)
        } catch (e: Exception) {
            L.e(e.message)
        }

        try {
            followContext?.destroy()
        } catch (e: Exception) {
            L.e(e.message)
        }

        followContext = null
        followSocket = null
    }

    /**
     * 主控端是否已经离开
     */
    fun isControlLeave(): Boolean {
        return System.currentTimeMillis() - lastCmdMillis > STREAM_IDLE_MILLIS
    }
}