package com.itant.rt.storage

/**
 * SharedPreference配置清单
 */
object KeyValue {
    /**
     * 本地列表数据
     */
    var followJsonString by MM("follow_json", "")

    /**
     * 客户端往这个端口推流
     */
    var serverPortStreamReceive by MM("server_port_stream_receive", 6899)

    /**
     * 客户端从这个端口拉流
     */
    var serverPortStreamPublish by MM("server_port_stream_publish", 4245)

    /**
     * 服务器从这个端口接收指令
     */
    var serverPortCmdReceive by MM("server_port_cmd_receive", 6898)
    /**
     * 服务器通过这个端口发送指令
     */
    var serverPortCmdSend by MM("server_port_cmd_send", 4244)

    /**
     * 设备ID
     */
    var deviceId by MM("deviceId", "")

    /**
     * 视频质量
     */
    var videoQuality by MM("videoQuality", 6)

    /**
     * 服务器IP
     */
    var serverIp by MM("serverIp", "192.168.0.1")

    /**
     * 视频最小宽高
     */
    var videoMaxSize by MM("videoMaxSize", 8192)
}