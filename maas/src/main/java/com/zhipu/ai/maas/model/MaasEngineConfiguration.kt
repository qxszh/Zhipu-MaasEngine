package com.zhipu.ai.maas.model

import android.content.Context
import com.zhipu.ai.maas.MaasConstants
import com.zhipu.ai.maas.MaasEngineEventHandler

class MaasEngineConfiguration(
    var context: Context?,
    var eventHandler: MaasEngineEventHandler?,
    var enableConsoleLog: Boolean,
    var enableSaveLogToFile: Boolean,
    var appId: String,
    var userId: Int,
    var rtcToken: String,
    var enableMultiTurnShortTermMemory: Boolean,
    var userName: String,
    var agentVoiceName: String,
    var input: SceneMode,
    var output: SceneMode,
    var vadConfiguration: VadConfiguration,
    var noiseEnvironment: MaasConstants.NoiseEnvironment = MaasConstants.NoiseEnvironment.NOISE,
    var speechRecognitionCompletenessLevel: MaasConstants.SpeechRecognitionCompletenessLevel = MaasConstants.SpeechRecognitionCompletenessLevel.NORMAL
) {
    constructor() : this(
        context = null,
        eventHandler = null,
        enableConsoleLog = false,
        enableSaveLogToFile = false,
        appId = "",
        rtcToken = "",
        userId = 0,
        enableMultiTurnShortTermMemory = false,
        userName = "",
        input = SceneMode("zh-CN", 16000, 1, 16),
        output = SceneMode("zh-CN", 16000, 1, 16),
        agentVoiceName = "",
        vadConfiguration = VadConfiguration(500)
    ) {

    }
}