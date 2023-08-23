package com.itant.rt.ui.home

import com.google.gson.reflect.TypeToken
import com.itant.rt.storage.KeyValue
import com.itant.rt.ui.follow.FollowBean
import com.itant.rt.ui.follow.FollowManager
import com.miekir.mvp.common.log.L
import com.miekir.mvp.presenter.BasePresenter
import com.miekir.mvp.task.launchModelTask


class HomePresenter: BasePresenter<HomeActivity>() {

    fun loadLocalFollow() {
        launchModelTask(
            {
                val list = ArrayList<FollowBean>()
                try {
                    val localList: List<FollowBean> = FollowManager.gson.fromJson(KeyValue.followJsonString, object : TypeToken<List<FollowBean>>() {}.type)
                    if (localList.isNotEmpty()) {
                        list.addAll(localList)
                    }
                } catch (e: Exception) {
                    L.e(e.message)
                }
                FollowManager.followLiveData.postValue(list)
            }
        )
    }

    fun saveLocalFollow(followList: List<FollowBean>) {
        launchModelTask(
            {
                KeyValue.followJsonString = FollowManager.gson.toJson(followList)
            }
        )
    }
}