package com.zhipu.realtime.model

data class SceneMode(
    val language: String,
    val speechFrameSampleRates: Int,
    val speechFrameChannels: Int,
    val speechFrameBits: Int
)
