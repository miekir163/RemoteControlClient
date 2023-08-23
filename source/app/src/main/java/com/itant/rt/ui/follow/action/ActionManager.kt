package com.itant.rt.ui.follow.action

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Path
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.ScreenUtils
import com.itant.rt.ui.follow.MessageReceiver
import com.itant.rt.utils.AppUtils
import com.miekir.mvp.common.context.GlobalContext
import com.miekir.mvp.common.log.L


/**
 * @date 2022-4-30 11:13
 * @author 詹子聪
 */
object ActionManager {
    const val TIME_START = 0L
    private const val DURATION_CLICK = 50L
    private const val DURATION_LONG_CLICK = 2000L
    private val screenWidth = ScreenUtils.getScreenWidth()
    private val screenHeight = ScreenUtils.getScreenHeight()

    @Volatile
    var service : ActionService? = null

    /**
     * 滑动
     */
    fun swipe(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long = DURATION_LONG_CLICK) {

        var startPixelX = startX * screenWidth
        var startPixelY = startY * screenHeight

        if (startPixelX < 10) {
            startPixelX = 1.0f
        } else if (startPixelX > screenWidth - 10) {
            startPixelX = screenWidth * 1.0f - 5
        }

        if (startPixelY < 10) {
            startPixelY = 1.0f
        } else if (startPixelY > screenHeight -10) {
            startPixelY = screenHeight * 1.0f - 5
        }

        var endPixelX = endX * screenWidth
        var endPixelY = endY * screenHeight

        if (endPixelX < 10) {
            endPixelX = 1.0f
        } else if (endPixelX > screenWidth - 10) {
            endPixelX = screenWidth * 1.0f - 5
        }

        if (endPixelY < 10) {
            endPixelY = 1.0f
        } else if (endPixelY > screenHeight -10) {
            endPixelY = screenHeight * 1.0f - 5
        }

        val path = Path().apply {
            moveTo(startPixelX, startPixelY)
            lineTo(endPixelX, endPixelY)
        }
        dispatchSwipeGesture(path, duration)
    }

    /**
     * 点击事件
     */
    fun click(x: Float, y: Float) {
        L.e("点击事件")

        var startPixelX = x * screenWidth
        if (startPixelX < 10) {
            startPixelX = 1.0f
        } else if (startPixelX > screenWidth -10) {
            startPixelX = screenWidth * 1.0f - 5
        }

        var startPixelY = y * screenHeight
        if (startPixelY < 10) {
            startPixelY = 1.0f
        } else if (startPixelY > screenHeight -10) {
            startPixelY = screenHeight * 1.0f - 5
        }

        GlobalContext.runOnUiThread {
            val clickPath = Path()
            clickPath.moveTo(startPixelX, startPixelY)
            val clickStroke = StrokeDescription(clickPath, TIME_START, DURATION_CLICK)
            val gestureDescription = GestureDescription.Builder().addStroke(clickStroke).build()
            service?.dispatchGesture(gestureDescription, null, null)
        }
    }

    fun longClick(x: Float, y: Float) {
        var startPixelX = x * screenWidth
        if (startPixelX < 10) {
            startPixelX = 1.0f
        } else if (startPixelX > screenWidth -10) {
            startPixelX = screenWidth * 1.0f - 5
        }

        var startPixelY = y * screenHeight
        if (startPixelY < 10) {
            startPixelY = 1.0f
        } else if (startPixelY > screenHeight -10) {
            startPixelY = screenHeight * 1.0f - 5
        }

        GlobalContext.runOnUiThread {
            val clickPath = Path()
            clickPath.moveTo(startPixelX, startPixelY)
            val clickStroke = StrokeDescription(clickPath, TIME_START, DURATION_LONG_CLICK)
            val gestureDescription = GestureDescription.Builder().addStroke(clickStroke).build()
            service?.dispatchGesture(gestureDescription, null, null)
        }
    }


    /**
     * 模拟手势滑动
     */
    private fun dispatchSwipeGesture(path: Path, duration: Long = DURATION_LONG_CLICK) {
        L.e("模拟手势滑动")
        val stroke = GestureDescription.StrokeDescription(path, TIME_START, duration)
        service?.dispatchGesture(
            GestureDescription.Builder().addStroke(stroke).build(), null, null
        )
    }

    /**
     * 回到桌面
     */
    fun goDesktop() {
        service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }

    /**
     * 模拟返回
     */
    fun goBack() {
        service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    /**
     * 模拟打开任务列表
     */
    fun openRecent() {
        service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
    }

    /**
     * 锁屏
     */
    fun lockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
        }
    }

    /**
     * 如果屏幕没亮则唤起屏幕
     */
    fun wakeupScreenIfNeed() {
        if (!isScreenOn() || MessageReceiver.isControlLeave()) {
            L.e("唤起屏幕")
            AppUtils.wakeupScreen()
        } else {
            L.e("屏幕已亮")
        }
    }

    /**
     * 当前屏幕是否亮了
     */
    private fun isScreenOn(): Boolean {
        val dm = GlobalContext.getContext().getSystemService(AppCompatActivity.DISPLAY_SERVICE) as DisplayManager
        for (display in dm.displays) {
            if (display.state != Display.STATE_ON) {
                return false
            }
        }
        return true
    }
}