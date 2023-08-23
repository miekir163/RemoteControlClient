package com.itant.rt.ui.home

import android.app.Activity
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.content.Intent
import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.itant.rt.R
import com.itant.rt.base.BaseActivity
import com.itant.rt.databinding.ItemFollowBinding
import com.itant.rt.ui.control.ControlActivity
import com.itant.rt.ui.edit.EditActivity
import com.itant.rt.ui.follow.FollowBean
import com.itant.rt.ui.follow.FollowManager
import com.miekir.mvp.common.autosize.adapt.AdaptAlertDialogBuilder
import com.miekir.mvp.common.context.GlobalContext
import com.miekir.mvp.common.extension.openActivity
import com.miekir.mvp.common.extension.setSingleClick
import com.miekir.mvp.view.result.ActivityResult


/**
 * 首页适配器
 */
class HomeAdapter(private val mList: MutableList<FollowBean>): BaseQuickAdapter<FollowBean, BaseViewHolder>(R.layout.item_follow, data = mList) {

    override fun convert(holder: BaseViewHolder, item: FollowBean) {
        val viewRoot = holder.getView<View>(R.id.viewRoot)
        val binding = ItemFollowBinding.bind(viewRoot)

        binding.tvName.text = "${item.n}"
        binding.tvServerIp.text = "${item.p}"
        binding.cardDevice.setSingleClick {
            FollowManager.currentFollowBean = item
            (context as Activity).openActivity<ControlActivity>()
        }

        binding.cardDevice.setOnLongClickListener {
            showDeleteDialog(item)
            true
        }
    }

    private fun showDeleteDialog(followBean: FollowBean) {
        val dialog = AdaptAlertDialogBuilder(context as Activity, R.style.AdaptAlertDialog, BaseActivity.SIZE_IN_DP_WIDTH, true)
            .setMessage(String.format(context.getString(R.string.message_delete), "${followBean.n}[${followBean.p}]"))
            .setNegativeButton(context.getString(R.string.cancel), object : OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    dialog?.dismiss()
                }
            })
            .setNeutralButton(context.getString(R.string.edit), object : OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    dialog?.dismiss()
                    (context as HomeActivity?)?.run {
                        EditActivity.followBean = followBean
                        val editIntent = Intent(this, EditActivity::class.java)
                        openActivityForResult(editIntent, object : ActivityResult() {
                            override fun onResultOK(backIntent: Intent?) {
                                super.onResultOK(backIntent)
                                notifyDataSetChanged()
                                mPresenter.saveLocalFollow(mList)
                            }
                        })
                    }
                }
            })
            .setPositiveButton(context.getString(R.string.confirm), object : OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    dialog?.dismiss()
                    val index = mList.indexOf(followBean)
                    mList.remove(followBean)
                    notifyItemRemoved(index)
                    (context as HomeActivity?)?.mPresenter?.saveLocalFollow(mList)
                }
            })
            .create()
        dialog.show()
    }
}
