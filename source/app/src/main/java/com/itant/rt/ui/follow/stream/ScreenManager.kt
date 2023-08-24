package com.itant.rt.ui.follow.stream

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.view.PixelCopy
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.blankj.utilcode.util.ScreenUtils
import com.itant.rt.R
import com.itant.rt.ext.enableCommonStream
import com.itant.rt.storage.KeyValue
import com.itant.rt.ui.follow.FollowManager
import com.itant.rt.ui.follow.MessageReceiver
import com.itant.rt.ui.follow.action.ActionManager
import com.itant.rt.utils.AppUtils
import com.itant.rt.utils.CrashManager
import com.itant.rt.utils.ZipUtils
import com.miekir.mvp.common.context.GlobalContext
import com.miekir.mvp.common.log.L
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.io.ByteArrayOutputStream
import java.lang.Thread.UncaughtExceptionHandler
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


/**
 * 不具备网络穿透功能，所以打算这样:
 * Server仅接受两个客户端，且需要密钥验证
 * Client1 发送数据包给 Server， Server 转发给 Client2
 */
object ScreenManager {
    /**
     * 回收资源专用线程
     */
    val recycleExecutorService: ExecutorService = Executors.newSingleThreadExecutor()

    val displayWidth = ScreenUtils.getScreenWidth()
    val displayHeight = ScreenUtils.getScreenHeight()

    val totalCountLiveData = MutableLiveData<Int>()
    val pushedCountLiveData = MutableLiveData<Int>()
    private const val MAX_COUNT = 10000

    @Volatile
    private var pushedCount = 0

    @Volatile
    private var totalCount = 0

    @Volatile
    var mProjectionIntent: Intent? = null

    /**
     * 是否已打开无障碍权限
     */
    val accessibilityEnabled: Boolean
        get() {
            return ActionManager.service != null
        }

    /**
     * 在单独的线程推送视频流，只拿最新的3帧
     */
    private val publishStreamExecutorService = ThreadPoolExecutor(1, 1, 10, TimeUnit.SECONDS, ArrayBlockingQueue(2), ThreadPoolExecutor.DiscardOldestPolicy())

    /**
     * 专门的一个线程用于截屏，防止卡顿
     */
    private val imageExecutorService = Executors.newSingleThreadExecutor()

    /**
     * 专门的一个线程池用于渲染视频流
     */
    val renderExecutorService: ExecutorService = Executors.newFixedThreadPool(2)

    @Volatile
    private var publishStreamSocket: ZMQ.Socket? = null

    @Volatile
    private var publishStreamContext: ZContext? = null

    @Volatile
    private var publishStreamUrl = ""

    /**
     * 专门的子线程用于截屏回调
     */
    @Volatile
    var threadHandler: Handler? = null

    @Volatile
    var mediaProjection: MediaProjection? = null

    @Volatile
    var virtualDisplay: VirtualDisplay? = null
    private const val flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR

    /**
     * 设置为成员变量，解决大量报错dequeueBuffer: BufferQueue has been abandoned
     */
    @Volatile
    var captureImageReader: ImageReader? = null

    @Volatile
    var lastBitmap: Bitmap? = null

    /**
     * 是否正在拷贝Bitmap
     */
    @Volatile
    private var copying = false

    /**
     * 是否要追加最后一帧截图
     */
    @Volatile
    private var lastFrameAppend = false

    @SuppressLint("WrongConstant")
    @Synchronized
    private fun createVirtualDisplay() {
        if (captureImageReader != null || virtualDisplay != null) {
            copying = true
            val rawBitmap = Bitmap.createBitmap(displayWidth, displayHeight, Bitmap.Config.ARGB_8888)
            val listener = PixelCopy.OnPixelCopyFinishedListener { copyResult ->
                var needRecycleRawBitmap = true
                when (copyResult) {
                    PixelCopy.SUCCESS -> {
                        if (rawBitmap.sameAs(lastBitmap)) {
                            // 图像相同不用传，省很多流量
                            L.e("图像相同")
                        } else {
                            lastBitmap?.recycle()
                            lastBitmap = rawBitmap
                            needRecycleRawBitmap = false

                            val screenMaxSize = Math.max(rawBitmap.width, rawBitmap.height)
                            val settingMaxSize = KeyValue.videoMaxSize

                            var scaledBitmap: Bitmap? = null
                            if (screenMaxSize > settingMaxSize) {
                                // 需要压缩到指定尺寸
                                var targetWidth = rawBitmap.width
                                var targetHeight = rawBitmap.height
                                if (targetWidth > targetHeight) {
                                    targetWidth = settingMaxSize
                                    val scale = rawBitmap.width * 1.0f / targetWidth
                                    targetHeight = (rawBitmap.height/scale).toInt()
                                } else {
                                    targetHeight = settingMaxSize
                                    val scale = rawBitmap.height * 1.0f / targetHeight
                                    targetWidth = (rawBitmap.width / scale).toInt()
                                }
                                if (targetWidth <= 0 || targetHeight <= 0) {
                                    L.e("非法宽高：${targetWidth}, ${targetHeight}")
                                } else {
                                    scaledBitmap = Bitmap.createScaledBitmap(rawBitmap, targetWidth, targetHeight, false)
                                }
                            }

                            val stream = ByteArrayOutputStream()
                            if (scaledBitmap != null) {
                                scaledBitmap.compress(Bitmap.CompressFormat.WEBP, KeyValue.videoQuality, stream)
                                scaledBitmap.recycle()
                            } else {
                                rawBitmap.compress(Bitmap.CompressFormat.WEBP, KeyValue.videoQuality, stream)
                            }

                            // 推送屏幕画面数据
                            val screenData = stream.toByteArray()
                            publishScreenData(screenData)
                        }
                    }

                    else -> {
                        L.e("Pixel copy failed with result $copyResult")
                    }
                }

                if (needRecycleRawBitmap) {
                    rawBitmap.recycle()
                }

                if (lastFrameAppend) {
                    lastFrameAppend = false
                    createVirtualDisplay()
                } else {
                    copying = false
                }
            }

            try {
                PixelCopy.request(captureImageReader?.surface!!, rawBitmap, listener, threadHandler!!)
            } catch (e: Exception) {
                copying = false
                rawBitmap.recycle()
                L.e("copy exception: ${e.message}")
            }
            L.e("end")
        } else {
            captureImageReader = ImageReader.newInstance(
                ScreenManager.displayWidth,
                ScreenManager.displayHeight,
                PixelFormat.RGBA_8888,
                1
            )
            if (mediaProjection == null && mProjectionIntent != null) {
                try {
                    val mediaProjectionManager = GlobalContext.getContext().getSystemService(MediaProjectionManager::class.java)
                    mediaProjection = mediaProjectionManager.getMediaProjection(
                        AppCompatActivity.RESULT_OK,
                        ScreenManager.mProjectionIntent!!
                    )
                } catch (e: Exception) {
                    L.e(e.message)
                    mediaProjection = null
                    recycleCapture()
                }
            }

            try {
                ScreenManager.virtualDisplay = mediaProjection?.createVirtualDisplay(
                    "ScreenCapture",
                    ScreenManager.displayWidth,
                    ScreenManager.displayHeight,
                    //ScreenUtils.getScreenDensity().toInt(),
                    Resources.getSystem().displayMetrics.densityDpi,
                    flags,
                    captureImageReader?.surface,
                    null,
                    ScreenManager.threadHandler
                )
            } catch (e: Exception) {
                recycleCapture()
            }
        }
    }

    /**
     * 回收截图资源
     */
    fun recycleCapture() {
        if (captureImageReader != null) {
            try {
                captureImageReader?.close()
            } catch (e: Exception) {
                L.e(e.message)
            } finally {
                captureImageReader = null
            }
        }

        if (virtualDisplay != null) {
            try {
                virtualDisplay?.surface?.release()
            } catch (e: Exception) {
                L.e(e.message)
            }

            try {
                virtualDisplay?.release()
            } catch (e: Exception) {
                L.e(e.message)
            } finally {
                virtualDisplay = null
            }
        }
    }

    /**
     * 开始共享屏幕
     */
    fun startShareScreen(intent: Intent) {
        mProjectionIntent = intent
        GlobalContext.getContext().run {
            val screenIntent = Intent(this, ScreenService::class.java)
            ContextCompat.startForegroundService(this, screenIntent)
        }
        totalCount = 0
        pushedCount = 0
        totalCountLiveData.postValue(totalCount)
        pushedCountLiveData.postValue(pushedCount)
    }

    /**
     * 停止共享屏幕
     */
    fun stopShareScreen() {
        GlobalContext.getContext().run {
            val screenIntent = Intent(this, ScreenService::class.java)
            stopService(screenIntent)
        }
        totalCount = 0
        pushedCount = 0
        totalCountLiveData.postValue(totalCount)
        pushedCountLiveData.postValue(pushedCount)
    }

    /**
     * 推流连接服务器
     */
    fun startPublishStreamService() {
        publishStreamUrl = "tcp://${FollowManager.currentFollowBean.p}:${FollowManager.currentFollowBean.p1}"
        publishStreamExecutorService.submit(Runnable {
            if (publishStreamSocket != null) {
                return@Runnable
            }
            try {
                publishStreamContext = ZContext()
                publishStreamContext?.setNotificationExceptionHandler(object : UncaughtExceptionHandler {
                    override fun uncaughtException(t: Thread, e: Throwable) {
                        L.e(e.message)
                        CrashManager.writeExceptionString(e.message)
                    }
                })
                publishStreamContext?.setUncaughtExceptionHandler(object : UncaughtExceptionHandler {
                    override fun uncaughtException(t: Thread, e: Throwable) {
                        L.e(e.message)
                        CrashManager.writeExceptionString(e.message)
                    }
                })
                publishStreamSocket = publishStreamContext?.createSocket(SocketType.PUB)
                publishStreamSocket?.enableCommonStream()
                publishStreamSocket?.connect(publishStreamUrl)
            } catch (e: Exception) {
                L.e(e.message)
                AppUtils.killSelf(
                    GlobalContext.getContext().getString(R.string.follow_start_failed)
                )
            }

            imageExecutorService.submit(Runnable {
                while (publishStreamSocket != null) {
                    val timeElapse = System.currentTimeMillis() - lastImageMillis
                    if (timeElapse < IMAGE_IDLE_MILLIS) {
                        try {
                            Thread.sleep(IMAGE_IDLE_MILLIS - timeElapse)
                        } catch (e: Exception) {
                            L.e(e.message)
                        }
                        continue
                    }

                    lastImageMillis = System.currentTimeMillis()
                    // 没打开无障碍权限或者有控制指令都要推流
                    if (!accessibilityEnabled || !MessageReceiver.isControlLeave()) {
                        if (copying) {
                            lastFrameAppend = true
                        } else {
                            createVirtualDisplay()
                        }
                    } else {
                        if (!copying) {
                            try {
                                Thread.sleep(2000)
                            } catch (e: Exception) {
                                L.e(e.message)
                            }
                            // 不需要当前设备推流的时候停止录屏，提高性能
                            mediaProjection?.stop()
                            mediaProjection = null
                            recycleCapture()
                        }
                    }
                }
            })
        })
    }

    fun recyclePublishStreamService() {
        try {
            publishStreamSocket?.disconnect(publishStreamUrl)
        } catch (e: Exception) {
            L.e(e.message)
        }

        try {
            publishStreamContext?.destroy()
        } catch (e: Exception) {
            L.e(e.message)
        }

        publishStreamSocket = null
        publishStreamContext = null
    }

    /**
     * 获取图片的时间差至少要隔60ms以上
     */
    private const val IMAGE_IDLE_MILLIS = 150
    @Volatile
    private var lastImageMillis = 0L

    /**
     * 在单独的线程推送视频流
     */
    private fun publishScreenData(screenData: ByteArray) {
        if (totalCount > MAX_COUNT) {
            totalCount = 0
            pushedCount = 0
        }
        totalCount++
        totalCountLiveData.postValue(totalCount)
        publishStreamExecutorService.submit(Runnable {
            pushedCount++
            pushedCountLiveData.postValue(pushedCount)
            val zipData = ZipUtils.jzlib(screenData)
            L.e("录屏", "推流大小：${zipData.size}")
            try {
                publishStreamSocket?.send(zipData)
            } catch (e: Exception) {
                L.e(e.message)
            }
        })
    }
}