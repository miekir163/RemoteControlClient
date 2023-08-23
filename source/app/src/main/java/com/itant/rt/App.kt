package com.itant.rt

import android.app.Application
import com.blankj.utilcode.util.AppUtils
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.miekir.mvp.MvpManager
import com.miekir.mvp.common.log.L
import com.miekir.mvp.task.net.RetrofitManager
import com.miekir.mvp.view.anim.SlideAnimation
import com.tencent.bugly.crashreport.CrashReport
import com.tencent.mmkv.MMKV

/**
 * @date 2021-8-28 15:26
 * @author 詹子聪
 */
class App: Application() {
    override fun onCreate() {
        super.onCreate()

        CrashReport.setIsDevelopmentDevice(applicationContext, true)
        CrashReport.initCrashReport(applicationContext, "bbdce09053", true)

        // 初始化本地存储
        val rootDir = MMKV.initialize(this)

        // MVP相关设置
        MvpManager.getInstance().activityAnimation(SlideAnimation())
        RetrofitManager.getDefault()
            .addInterceptors(ChuckerInterceptor.Builder(this).build())
            .printLog(AppUtils.isAppDebug())
    }
}