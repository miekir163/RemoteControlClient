package com.itant.rt.ui.home

import android.Manifest
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.itant.rt.BuildConfig
import com.itant.rt.R
import com.itant.rt.base.BaseActivity
import com.itant.rt.databinding.ActivityHomeBinding
import com.itant.rt.ui.follow.FollowActivity
import com.itant.rt.ui.follow.FollowBean
import com.itant.rt.ui.follow.FollowManager
import com.itant.rt.ui.scanhw.HwScanActivity
import com.miekir.mvp.common.extension.lazy
import com.miekir.mvp.common.extension.openActivity
import com.miekir.mvp.common.extension.setSingleClick
import com.miekir.mvp.common.tools.ToastTools
import com.miekir.mvp.view.result.ActivityResult

/**
 * 首页
 */
class HomeActivity : BaseActivity<ActivityHomeBinding>() {

    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> }

    private val mList = ArrayList<FollowBean>()
    private val mAdapter = HomeAdapter(mList)
    val mPresenter: HomePresenter by lazy()

    override fun onBindingInflate() = ActivityHomeBinding.inflate(layoutInflater)

    override fun onInit() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        binding.rvFollow.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = mAdapter
        }

        FollowManager.followLiveData.observe(this) {
            if (!it.isNullOrEmpty()) {
                mList.addAll(0, it)
                mAdapter.notifyDataSetChanged()
            }
            mPresenter.saveLocalFollow(mList)
        }

        binding.btnControl.setSingleClick {
            requestPermissionsForResult(Manifest.permission.CAMERA) { granted, temp, detail ->
                if (granted) {
                    //openActivity<ScanActivity>()
                    openActivity<HwScanActivity>()
                } else {
                    ToastTools.showShort(getString(R.string.permission_required))
                }
            }
        }

        binding.btnFollow.setSingleClick {
            val powerManager = getSystemService(Service.POWER_SERVICE) as PowerManager
            val hasBeenIgnore = powerManager.isIgnoringBatteryOptimizations(packageName)
            if (hasBeenIgnore) {
                openActivity<FollowActivity>()
                return@setSingleClick
            }

            try {
                val batteryIntent = Intent().apply {
                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    data = Uri.parse("package:$packageName")
                }
                openActivityForResult(batteryIntent, object: ActivityResult() {
                    override fun onResultOK(backIntent: Intent?) {
                        super.onResultOK(backIntent)
                        openActivity<FollowActivity>()
                    }

                    override fun onResultFail(code: Int) {
                        super.onResultFail(code)
                        openActivity<FollowActivity>()
                    }
                })
            } catch (e: Exception) {
                ToastTools.showLong(getString(R.string.tips_long_run))
                openActivity<FollowActivity>()
            }
        }

        binding.tvTitle.text = getString(R.string.app_name)

        mPresenter.loadLocalFollow()
    }
}