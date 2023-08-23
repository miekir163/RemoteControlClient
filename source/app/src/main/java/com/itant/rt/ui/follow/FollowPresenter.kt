package com.itant.rt.ui.follow

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import com.blankj.utilcode.util.ImageUtils
import com.itant.rt.R
import com.king.zxing.util.CodeUtils
import com.miekir.mvp.common.context.GlobalContext
import com.miekir.mvp.common.tools.ToastTools
import com.miekir.mvp.presenter.BasePresenter
import com.miekir.mvp.task.launchModelTask

class FollowPresenter: BasePresenter<FollowActivity>() {

    fun genQr(followBean: FollowBean) {
        launchModelTask(
            {
                CodeUtils.createQRCode(FollowManager.gson.toJson(followBean), 800, Color.parseColor("#3C4043"))
            }, onSuccess = { bitmap ->
                view?.run {
                    binding.ivQr.setImageBitmap(bitmap)

                    binding.ivQr.setOnLongClickListener {
                        saveImage(bitmap)
                        true
                    }
                }
            }
        )
    }

    fun saveBitmap2Album(bitmap: Bitmap?) {
        launchModelTask(
            {
                ImageUtils.save2Album(bitmap, Bitmap.CompressFormat.PNG)
            }
        )
        ToastTools.showShort(GlobalContext.getContext().getString(R.string.image_saved))
    }
}