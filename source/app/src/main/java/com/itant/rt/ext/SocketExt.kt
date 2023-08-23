package com.itant.rt.ext

import org.zeromq.ZMQ

/**
 * 推拉流设置，把延迟控制在5秒内
 */
fun  ZMQ.Socket.enableCommonStream() {
    enableCommonConnect()
    receiveBufferSize = 100*1024
    hwm = 2
}

/**
 * 聊天设置
 */
fun ZMQ.Socket.enableCommonChat() {
    enableCommonConnect()
    sendTimeOut = 5000
    receiveTimeOut = 5000
}

/**
 * 重连
 */
private fun ZMQ.Socket.enableCommonConnect() {
    setTCPKeepAlive(1)
    setTCPKeepAliveIdle(20)
    setTCPKeepAliveInterval(10)
    // 心跳周期
    setHeartbeatIvl(2*1000)
    // 心跳超时
    setHeartbeatTimeout(5*1000)
    // 超过则会让连接超时
    setHeartbeatTtl(10*1000)
    setReconnectIVL(500)
    setReconnectIVLMax(1000)
}