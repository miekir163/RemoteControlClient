package com.itant.rt.ui.follow

import com.google.gson.Gson
import com.miekir.mvp.common.component.lifecycle.LiveDataInstant

object FollowManager {
    val gson = Gson()

    var currentFollowBean: FollowBean = FollowBean()

    val followLiveData = LiveDataInstant<List<FollowBean>?>()
    val serverLiveData = LiveDataInstant<String>()
}