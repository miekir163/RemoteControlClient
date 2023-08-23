package com.itant.rt.utils

import com.miekir.mvp.common.context.GlobalContext
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors


object CrashManager {
    private val crashExecutorService = Executors.newSingleThreadExecutor()

    private val logDir = File(GlobalContext.getContext().externalCacheDir, "crash")

    /**
     * 无需读写权限，写到/sdcard/Android/data/包名/cache/crash文件夹下
     */
    fun writeExceptionString(message: String?) {
        crashExecutorService.submit(Runnable {
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            val crashFile = File(logDir, "crash.log")
            if (!crashFile.exists()) {
                crashFile.createNewFile()
            }
            crashFile.appendText("${message}\n", StandardCharsets.UTF_8)
        })
    }
}