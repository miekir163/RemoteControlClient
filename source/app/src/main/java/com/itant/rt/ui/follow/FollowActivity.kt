package com.itant.rt.ui.follow

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.RegexUtils
import com.blankj.utilcode.util.ServiceUtils
import com.itant.rt.R
import com.itant.rt.base.BaseActivity
import com.itant.rt.databinding.ActivityFollowBinding
import com.itant.rt.storage.KeyValue
import com.itant.rt.ui.follow.action.ActionService
import com.itant.rt.ui.follow.stream.ScreenManager
import com.itant.rt.ui.follow.stream.ScreenService
import com.miekir.mvp.common.autosize.adapt.AdaptAlertDialogBuilder
import com.miekir.mvp.common.extension.lazy
import com.miekir.mvp.common.extension.setSingleClick
import com.miekir.mvp.common.log.L
import com.miekir.mvp.common.tools.ToastTools
import com.tencent.bugly.crashreport.CrashReport


class FollowActivity: BaseActivity<ActivityFollowBinding>() {

    private val mediaProjectionRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.run {
            if (result.resultCode == RESULT_OK) {
                // 开启服务
                ScreenManager.startShareScreen(this)
                showControlPermissionDialog()
            } else {
                ToastTools.showShort(getString(R.string.permission_required))
                isRunning = false
            }
        }
    }

    private val presenter: FollowPresenter by lazy()

    /**
     * 弹出对话框请求无障碍权限，否则只能查看不能控制
     */
    private fun showControlPermissionDialog() {
        if (ScreenManager.accessibilityEnabled) {
            return
        }
        AdaptAlertDialogBuilder(this, R.style.AdaptAlertDialog, SIZE_IN_DP_WIDTH, true)
            .setMessage(getString(R.string.accessibility_message))
            .setNegativeButton(getString(R.string.cancel)) { dialog, which -> dialog?.dismiss() }
            .setPositiveButton(getString(R.string.grant)) { dialog, which ->
                dialog?.dismiss()
                ToastTools.showLong(getString(R.string.accessibility_required))

                try {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                Intent.FLAG_ACTIVITY_NO_HISTORY or
                                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    }
                    intent.putComponent(packageName, ActionService::class.java)

                    startActivity(intent)
                } catch (e: Exception) {
                    L.e(e.message)
                }
            }
            .create()
            .show()
    }

    private fun Intent.putComponent(pkg: String, cls: Class<*>) {
        val cs = ComponentName(pkg, cls.name).flattenToString()
        val bundle = Bundle()
        bundle.putString(":settings:fragment_args_key", cs)
        putExtra(":settings:fragment_args_key", cs)
        putExtra(":settings:show_fragment_args", bundle)
    }

    /**
     * 解析密钥，生成二维码，二维码内容为FollowBean JSON
     * ip~md5~key
     * 192.168.0.1~60cc374626d2bc51~1,2,3,4
     */
    private fun parseSecretAndShowQr() {
        binding.ivQr.visibility = View.VISIBLE
        
        // 设备ID
        if (TextUtils.isEmpty(KeyValue.deviceId)) {
            KeyValue.deviceId = "${System.currentTimeMillis()}"
        }

        // 通讯密钥解析
        val followBean = FollowBean().apply {
            i = KeyValue.deviceId
            n = Build.PRODUCT
            p = KeyValue.serverIp
        }

        FollowManager.currentFollowBean = followBean
        presenter.genQr(followBean)

        if (!forInit) {
            startFollow()
        }

        CrashReport.setDeviceId(applicationContext, KeyValue.deviceId)
        CrashReport.setDeviceModel(applicationContext, Build.PRODUCT)
    }

    override fun onBindingInflate() = ActivityFollowBinding.inflate(layoutInflater)

    private var forInit = true
    private var isRunning: Boolean = false
        set(value) {
            field = value
            if (value) {
                binding.btnStartStop.text = getString(R.string.stop)
                binding.ivQr.visibility = View.VISIBLE
                // 解析密钥并展示二维码
                parseSecretAndShowQr()
            } else {
                binding.btnStartStop.text = getString(R.string.start)
                binding.ivQr.visibility = View.GONE
                if (!forInit) {
                    stopFollow()
                }
            }
        }

    private fun startFollow() {
        val mediaProjectionManager = getSystemService(MediaProjectionManager::class.java)
        mediaProjectionRequest.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    private fun stopFollow() {
        ScreenManager.stopShareScreen()
    }

    override fun onInit() {
        if (ServiceUtils.isServiceRunning(ScreenService::class.java)) {
            isRunning = true
        }
        forInit = false

        binding.etIp.visibility = View.VISIBLE
        binding.etIp.setText(KeyValue.serverIp)
        binding.tvDescription.visibility = View.GONE

        binding.btnStartStop.setSingleClick {
            if (!isRunning) {
                val ipString = binding.etIp.text.toString()
                if (!RegexUtils.isIP(ipString)) {
                    ToastTools.showShort(getString(R.string.invalid_ip))
                    return@setSingleClick
                }
                KeyValue.serverIp = ipString
            }
            isRunning = !isRunning
        }
    }

    override fun onBackPressed() {
        if (isRunning) {
            ActivityUtils.startHomeActivity()
        } else {
            super.onBackPressed()
        }
    }

    fun saveImage(bitmap: Bitmap?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            requestPermissionsForResult(Manifest.permission.WRITE_EXTERNAL_STORAGE) { granted, temp, detail ->
                if (granted) {
                    presenter.saveBitmap2Album(bitmap)
                } else {
                    ToastTools.showShort(getString(R.string.permission_required))
                }
            }
        } else {
            presenter.saveBitmap2Album(bitmap)
        }
    }
}