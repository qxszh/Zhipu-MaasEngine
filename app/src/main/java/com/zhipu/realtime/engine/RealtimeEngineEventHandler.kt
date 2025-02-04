package com.zhipu.realtime.engine

import com.zhipu.realtime.model.AudioVolumeInfo


interface RealtimeEngineEventHandler {
    fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int)
    fun onLeaveChannelSuccess()
    fun onUserJoined(uid: Int, elapsed: Int)
    fun onUserOffline(uid: Int, reason: Int)
    fun onAudioVolumeIndication(
        speakers: ArrayList<AudioVolumeInfo>,
        totalVolume: Int
    )
    fun onStreamMessage(uid: Int, data: ByteArray?)
}