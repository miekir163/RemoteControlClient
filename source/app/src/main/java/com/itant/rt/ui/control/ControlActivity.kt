package com.itant.rt.ui.control

import android.app.Dialog
import android.app.ProgressDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.itant.rt.R
import com.itant.rt.base.BaseActivity
import com.itant.rt.databinding.ActivityControlBinding
import com.itant.rt.ext.enableCommonStream
import com.itant.rt.ui.follow.FollowBean
import com.itant.rt.ui.follow.FollowManager
import com.itant.rt.ui.follow.action.ActionListener
import com.itant.rt.ui.follow.action.ActionManager
import com.itant.rt.ui.follow.action.ActionType
import com.itant.rt.ui.follow.stream.ScreenManager
import com.itant.rt.ui.setting.SettingActivity
import com.itant.rt.utils.ZipUtils
import com.miekir.mvp.common.context.GlobalContext
import com.miekir.mvp.common.extension.openActivity
import com.miekir.mvp.common.extension.setSingleClick
import com.miekir.mvp.common.log.L
import com.miekir.mvp.common.tools.ToastTools
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ


/**
 * 接收远程屏幕界面
 */
class ControlActivity: BaseActivity<ActivityControlBinding>() {
    companion object {
        private const val TAG = "CONTROL"
        private const val WHAT_TIMEOUT = -1
    }

    @Volatile
    private var mZContext: ZContext? = null
    private lateinit var mSocket: ZMQ.Socket
    private val matrix = Matrix()
    private val options = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    @Volatile
    private var mHandler: Handler? = object: Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.what == WHAT_TIMEOUT) {
                ToastTools.showShort(getString(R.string.timeout))
                finish()
            } else {
                binding.tvCount.text = "${msg.what}"
            }
        }
    }
    private lateinit var loadingDialog: Dialog

    private val serverUrl = "tcp://${FollowManager.currentFollowBean.p}:${FollowManager.currentFollowBean.p2}"

    override fun onBindingInflate() = ActivityControlBinding.inflate(layoutInflater)

    private val lifeCycleListener = object : VideoTextureView.ITextureViewLifeCycleListener {
        override fun available() {
            ScreenManager.renderExecutorService.execute  {
                startPreviewVideo()
            }
        }

        override fun destroy() {
            ScreenManager.renderExecutorService.submit {
                release()
            }
        }
    }

    override fun onInit() {
        loadingDialog = ProgressDialog(this, R.style.AdaptAlertDialog).apply {
            setCancelable(true)
            setCanceledOnTouchOutside(false)
            setMessage(getString(R.string.connecting))
            setOnCancelListener { mHandler?.removeMessages(WHAT_TIMEOUT) }
        }
        loadingDialog.show()
        mHandler?.sendEmptyMessageDelayed(WHAT_TIMEOUT, 20_000L)

        binding.ivClose.setSingleClick {
            MessageSender.sendCmdToExplicitDevice(ActionType.CONTROL_LEAVE, 0.0f, 0.0f, 0.0f, 0.0f, ActionManager.TIME_START, FollowManager.currentFollowBean.i)
            finish()
        }
        binding.ivHome.setSingleClick {
            MessageSender.sendCmd(ActionType.DESKTOP, 0.0f, 0.0f, 0.0f, 0.0f, 0)
        }
        binding.ivBack.setSingleClick {
            MessageSender.sendCmd(ActionType.BACK, 0.0f, 0.0f, 0.0f, 0.0f, 0)
        }
        binding.ivOptions.setSingleClick {
            MessageSender.sendCmd(ActionType.OPTIONS, 0.0f, 0.0f, 0.0f, 0.0f, 0)
        }
        binding.ivSetting.setSingleClick { openActivity<SettingActivity>() }

        binding.svRemote.setLifeCycleListener(lifeCycleListener)
        MessageSender.startControl()

        val touchListener = ActionListener(binding.svRemote)
        binding.svRemote.setOnTouchListener(touchListener)

        binding.svRemote.setOnLongClickListener {
            touchListener.performLongClick(binding.svRemote)
            true
        }
    }

    @Volatile
    private var count = 0
    @Volatile
    private var paramsSet = false
    @Volatile
    private var latestBitmap: Bitmap? = null
    @Volatile
    private var lastBitmapWidth = 0
    @Volatile
    private var lastBitmapHeight = 0
    private fun startPreviewVideo() {
        count = 0
        mZContext = ZContext()
        mSocket = mZContext!!.createSocket(SocketType.SUB)
        mSocket.enableCommonStream()

        mSocket.connect(serverUrl) //ZeroMQ 推流
        mSocket.subscribe("".toByteArray())
        val bitmapPaint = Paint()

        while (mZContext != null && !mZContext!!.isClosed && !Thread.currentThread().isInterrupted) {
            // Block until a message is received
            try {
                val receivedData = mSocket.recv()
                if(receivedData == null || receivedData.isEmpty()) {
                    continue
                }

                val imageData = ZipUtils.unJzlib(receivedData)
                val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size, options)
                if (lastBitmapWidth != bitmap.width || lastBitmapHeight != bitmap.height) {
                    paramsSet = false
                    lastBitmapWidth = bitmap.width
                    lastBitmapHeight = bitmap.height
                    val widthScale = binding.svRemote.width * 1.0f / bitmap.width
                    val heightScale = binding.svRemote.height * 1.0f / bitmap.height
                    val scale = Math.min(widthScale, heightScale)
                    val layoutParams = binding.svRemote.layoutParams
                    layoutParams.width = (bitmap.width * scale).toInt()
                    layoutParams.height = (bitmap.height * scale).toInt()
                    GlobalContext.runOnUiThread {
                        binding.svRemote.layoutParams = layoutParams

                        if (loadingDialog.isShowing) {
                            loadingDialog.cancel()
                        }

                        paramsSet = true
                        latestBitmap?.run {
                            binding.svRemote.drawBitmap(this, bitmapPaint)
                            recycle()
                            latestBitmap = null
                        }

                        // 宽高发生改变，需要重新监听，否则触摸不准
                        val touchListener = ActionListener(binding.svRemote)
                        binding.svRemote.setOnTouchListener(touchListener)

                        binding.svRemote.setOnLongClickListener {
                            touchListener.performLongClick(binding.svRemote)
                            true
                        }
                    }
                    matrix.setScale(scale, scale)
                    MessageSender.sendCmd(ActionType.VIDEO_QUALITY, 0.0f, 0.0f, 0.0f, 1.0f, FollowManager.currentFollowBean.q.toLong())
                    MessageSender.sendCmd(ActionType.VIDEO_SIZE, 0.0f, 0.0f, 0.0f, 1.0f, FollowManager.currentFollowBean.s.toLong())
                }
                val scaledBitmap = Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.width,
                    bitmap.height,
                    matrix,
                    true
                )

                if (paramsSet) {
                    binding.svRemote.drawBitmap(scaledBitmap, bitmapPaint)
                    scaledBitmap.recycle()
                } else {
                    latestBitmap?.recycle()
                    latestBitmap = scaledBitmap
                }

                bitmap.recycle()

                if (count > 99) {
                    count = 0
                }
                count++
                mHandler?.sendEmptyMessage(count)
            } catch (e:Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun release() {
        if (this@ControlActivity::mSocket.isInitialized) {
            try {
                mSocket.disconnect(serverUrl)
            } catch (e: Exception) {
                L.e(e.message)
            }

            try {
                mSocket.close()
            } catch (e: Exception) {
                L.e(e.message)
            }

            try {
                mZContext?.destroy()
            } catch (e: Exception) {
                L.e(e.message)
            } finally {
                mZContext = null
            }
        }
    }

    override fun onBackPressed() {
        if (loadingDialog.isShowing) {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.svRemote.setOnTouchListener(null)
        MessageSender.stopControl()
        FollowManager.currentFollowBean = FollowBean()
        mHandler?.removeMessages(WHAT_TIMEOUT)
        mHandler = null
        latestBitmap?.recycle()
        latestBitmap = null
    }
}