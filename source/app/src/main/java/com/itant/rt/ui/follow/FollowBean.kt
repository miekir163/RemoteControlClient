package com.itant.rt.ui.follow

import androidx.annotation.Keep
import com.blankj.utilcode.util.RegexUtils
import com.itant.rt.storage.KeyValue

@Keep
class FollowBean {
    /**
     * Device ID
     */
    var i: String = ""
    /**
     * Device Name
     */
    var n: String = ""
    /**
     * Server IP
     * 服务器端写自己的IP，不要写localhost
     */
    var p: String = ""

    var p1: Int = KeyValue.serverPortStreamReceive
    var p2: Int = KeyValue.serverPortStreamPublish
    var p3: Int = KeyValue.serverPortCmdReceive
    var p4: Int = KeyValue.serverPortCmdSend

    /**
     * 视频质量
     */
    var q = KeyValue.videoQuality

    /**
     * 最大视频尺寸
     */
    var s = KeyValue.videoMaxSize

    fun isInvalid(): Boolean {
        return !RegexUtils.isIP(p)
    }
}
