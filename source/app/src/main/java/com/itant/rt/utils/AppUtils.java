package com.itant.rt.utils;

import android.app.Service;
import android.content.Context;
import android.os.PowerManager;
import android.text.TextUtils;

import com.itant.rt.ui.control.MessageSender;
import com.itant.rt.ui.follow.action.ActionManager;
import com.itant.rt.ui.follow.stream.ScreenManager;
import com.miekir.mvp.common.context.GlobalContext;
import com.miekir.mvp.common.log.L;
import com.miekir.mvp.common.tools.ToastTools;

public final class AppUtils {

    private AppUtils() {}

    public static void killSelf(String message) {
        L.e(message);
        ScreenManager.INSTANCE.stopShareScreen();
        MessageSender.INSTANCE.stopControl();
        Service service = ActionManager.INSTANCE.getService();
        if (service != null) {
            service.stopSelf();
        }
        com.blankj.utilcode.util.AppUtils.exitApp();
    }

    /**
     * 唤醒屏幕
     */
    public static void wakeupScreen() {
        PowerManager pm = (PowerManager) GlobalContext.getContext().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK|PowerManager.ACQUIRE_CAUSES_WAKEUP, "rt:wake_screen");
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire(10_000);
        wakeLock.release();
    }
}
