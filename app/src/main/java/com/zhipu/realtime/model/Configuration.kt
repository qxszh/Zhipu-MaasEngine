package com.zhipu.realtime.model

import android.content.Context
import com.zhipu.realtime.constants.RealtimeConstants
import com.zhipu.realtime.engine.RealtimeEngineEventHandler

class Configuration(
    var context: Context?,
    var eventHandler: RealtimeEngineEventHandler?,
    var enableConsoleLog: Boolean,
    var enableSaveLogToFile: Boolean,
    var appId: String,
    var userId: Int,
    var channelId: String,
    var rtcToken: String,
    var enableMultiTurnShortTermMemory: Boolean,
    var userName: String,
    var agentVoiceName: String,
    var input: SceneMode,
    var output: SceneMode,
    var vadConfiguration: VadConfiguration,
    var noiseEnvironment: RealtimeConstants.NoiseEnvironment = RealtimeConstants.NoiseEnvironment.NOISE,
    var speechRecognitionCompletenessLevel: RealtimeConstants.SpeechRecognitionCompletenessLevel = RealtimeConstants.SpeechRecognitionCompletenessLevel.NORMAL,
    var customSystemPrompt: String = "",
) {
    constructor() : this(
        context = null,
        eventHandler = null,
        enableConsoleLog = false,
        enableSaveLogToFile = false,
        appId = "",
        rtcToken = "",
        userId = 0,
        channelId = "",
        enableMultiTurnShortTermMemory = false,
        userName = "",
        input = SceneMode("zh-CN", 16000, 1, 16),
        output = SceneMode("zh-CN", 16000, 1, 16),
        agentVoiceName = "",
        vadConfiguration = VadConfiguration(500),
        customSystemPrompt = ""
    ) {

    }
}