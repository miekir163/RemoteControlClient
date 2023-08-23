package com.itant.rt.ui.scanhw

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import com.huawei.hms.hmsscankit.ScanUtil
import com.huawei.hms.ml.scan.HmsScan
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions
import com.itant.rt.ui.follow.FollowBean
import com.itant.rt.ui.follow.FollowManager
import com.miekir.mvp.common.log.L
import com.miekir.mvp.common.tools.ToastTools

class HwScanActivity : AppCompatActivity() {

    companion object {
        const val KEY_INTENT = "key_intent"
        private const val REQUEST_CODE_SCAN_ONE = 9
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // “QRCODE_SCAN_TYPE”和“DATAMATRIX_SCAN_TYPE”表示只扫描QR和DataMatrix的码，setViewType设置扫码标题，0表示设置扫码标题为“扫描二维码/条码”，1表示设置扫码标题为“扫描二维码”，默认为0; setErrorCheck设置错误监听，true表示监听错误并退出扫码页面，false表示不上报错误，仅检查到识别结果后退出扫码页面，默认为false
        val options = HmsScanAnalyzerOptions.Creator()
            .setHmsScanTypes(HmsScan.QRCODE_SCAN_TYPE)
            .setViewType(1)
            .setErrorCheck(false)
            .create()
        ScanUtil.startScan(this, REQUEST_CODE_SCAN_ONE, options)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null && requestCode == REQUEST_CODE_SCAN_ONE) {
            // 导入图片扫描返回结果
            val errorCode: Int = data.getIntExtra(ScanUtil.RESULT_CODE, ScanUtil.SUCCESS)
            if (errorCode == ScanUtil.SUCCESS) {
                val resultObj: HmsScan? = data.getParcelableExtra(ScanUtil.RESULT)
                if (resultObj != null && !TextUtils.isEmpty(resultObj.showResult)) {
                    // 展示扫码结果
                    onScanResult(resultObj.showResult)
                } else {
                    ToastTools.showShort("识别失败")
                }
            } else {
                ToastTools.showShort("识别失败")
            }
        }
        finish()
    }

    private fun onScanResult(barcodeString: String) {
        if (intent.getIntExtra(HwScanActivity.KEY_INTENT, 0) == 0) {
            // 获取扫描结果
            var followBean: FollowBean? = null
            try {
                followBean = FollowManager.gson.fromJson(barcodeString, FollowBean::class.java)
            } catch (e: Exception) {
                L.e(e.message)
            }
            if (followBean == null || followBean.isInvalid()) {
                ToastTools.showLong("无效的二维码")
            } else {
                val list = ArrayList<FollowBean>().apply {
                    add(followBean)
                }
                FollowManager.followLiveData.postValue(list)
            }
        }  else {
            FollowManager.serverLiveData.postValue(barcodeString)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {

    }

}