package com.itant.rt.ui.edit

import android.text.TextUtils
import com.itant.rt.R
import com.itant.rt.base.BaseActivity
import com.itant.rt.databinding.ActivityEditBinding
import com.itant.rt.ui.follow.FollowBean
import com.miekir.mvp.common.extension.setSingleClick
import com.miekir.mvp.common.tools.ToastTools

/**
 * 编辑设备
 */
class EditActivity: BaseActivity<ActivityEditBinding>() {
    override fun onBindingInflate() = ActivityEditBinding.inflate(layoutInflater)

    companion object {
        var followBean: FollowBean? = null
    }

    override fun onInit() {
        followBean?.run {
            binding.etName.setText(n)
            binding.etIp.setText(p)
        }

        binding.btnSave.setSingleClick {
            val name = binding.etName.text.toString()
            if (TextUtils.isEmpty(name)) {
                ToastTools.showShort(getString(R.string.name_required))
                return@setSingleClick
            }

            followBean?.apply { n = name }
            setResult(RESULT_OK)
            finish()
        }

        binding.ivClose.setSingleClick { finish() }
    }
}