package com.itant.rt.ui.follow.action

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.itant.rt.ui.control.startForegroundNotification

/**
 * 动作服务
 */
class ActionService : AccessibilityService() {
    override fun onCreate() {
        super.onCreate()
        ActionManager.service = this
    }

    override fun onDestroy() {
        super.onDestroy()
        ActionManager.service = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
    }
    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        return super.onKeyEvent(event)
    }
}