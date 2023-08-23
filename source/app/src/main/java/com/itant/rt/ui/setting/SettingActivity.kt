package com.itant.rt.ui.setting

import android.text.TextUtils
import com.itant.rt.R
import com.itant.rt.base.BaseActivity
import com.itant.rt.databinding.ActivitySettingBinding
import com.itant.rt.storage.KeyValue
import com.itant.rt.ui.control.MessageSender
import com.itant.rt.ui.follow.FollowManager
import com.itant.rt.ui.follow.action.ActionType
import com.miekir.mvp.common.extension.setSingleClick
import com.miekir.mvp.common.tools.ToastTools

class SettingActivity: BaseActivity<ActivitySettingBinding>() {
    override fun onBindingInflate() = ActivitySettingBinding.inflate(layoutInflater)

    override fun onInit() {
        binding.etVideoQuality.setText("${FollowManager.currentFollowBean.q}")
        binding.etVideoMaxSize.setText("${FollowManager.currentFollowBean.s}")

        binding.btnSet.setSingleClick {
            val qualityString = binding.etVideoQuality.text.toString()
            if (TextUtils.isEmpty(qualityString)) {
                ToastTools.showShort(getString(R.string.toast_quality))
                return@setSingleClick
            }
            val quality = qualityString.toInt()
            if (quality < 0 || quality > 100) {
                ToastTools.showShort(getString(R.string.toast_quality))
                return@setSingleClick
            }

            val videoSizeString = binding.etVideoMaxSize.text.toString()
            if (TextUtils.isEmpty(qualityString)) {
                ToastTools.showShort(getString(R.string.toast_size))
                return@setSingleClick
            }
            val videoSize = videoSizeString.toInt()
            if (videoSize < 320) {
                ToastTools.showShort(getString(R.string.toast_size))
                return@setSingleClick
            }

            FollowManager.currentFollowBean.q = quality
            FollowManager.currentFollowBean.s = videoSize
            FollowManager.followLiveData.postValue(null)
            ToastTools.showShort(getString(R.string.toast_set_success))
            MessageSender.sendCmd(ActionType.VIDEO_QUALITY, 0.0f, 0.0f, 0.0f, 1.0f, quality.toLong())
            MessageSender.sendCmd(ActionType.VIDEO_SIZE, 0.0f, 0.0f, 0.0f, 1.0f, videoSize.toLong())

            finish()
        }

        binding.ivClose.setSingleClick { finish() }
    }
}