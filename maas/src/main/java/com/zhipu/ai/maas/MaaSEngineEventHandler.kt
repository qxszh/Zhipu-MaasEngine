package com.zhipu.ai.maas

import com.zhipu.ai.maas.model.AudioVolumeInfo


interface MaaSEngineEventHandler {
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