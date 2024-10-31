package com.zhipu.ai.maas.model

data class AudioVolumeInfo(
    var uid: Int = 0,
    var volume: Int = 0,
    var vad: Int = 0,
    var voicePitch: Double = 0.0
)
