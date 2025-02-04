package com.zhipu.realtime.engine

import com.zhipu.realtime.model.WatermarkOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.video.VideoEncoderConfiguration

object RealtimeEngineOptionsHelper {
    fun getRtcFrameRate(frameRate: Int): VideoEncoderConfiguration.FRAME_RATE {
        return when (frameRate) {
            1 -> VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_1
            7 -> VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_7
            10 -> VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_10
            15 -> VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15
            24 -> VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_24
            30 -> VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_30
            60 -> VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_60
            else -> VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15
        }
    }

    fun getRtcOrientationMode(mode: Int): VideoEncoderConfiguration.ORIENTATION_MODE {
        return when (mode) {
            0 -> VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
            1 -> VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_LANDSCAPE
            2 -> VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
            else -> VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
        }
    }

    fun getRtcVideoModulePosition(position: Int): Constants.VideoModulePosition {
        return when (position) {
            1 -> Constants.VideoModulePosition.VIDEO_MODULE_POSITION_POST_CAPTURER
            2 -> Constants.VideoModulePosition.VIDEO_MODULE_POSITION_PRE_RENDERER
            4 -> Constants.VideoModulePosition.VIDEO_MODULE_POSITION_PRE_ENCODER
            8 -> Constants.VideoModulePosition.VIDEO_MODULE_POSITION_POST_CAPTURER_ORIGIN
            else -> Constants.VideoModulePosition.VIDEO_MODULE_POSITION_POST_CAPTURER
        }
    }

    fun getRtcWatermarkOptions(options: WatermarkOptions): io.agora.rtc2.video.WatermarkOptions {
        val rtcOptions = io.agora.rtc2.video.WatermarkOptions()
        rtcOptions.visibleInPreview = options.visibleInPreview
        rtcOptions.positionInLandscapeMode = io.agora.rtc2.video.WatermarkOptions.Rectangle(
            options.positionInLandscapeMode.x,
            options.positionInLandscapeMode.y,
            options.positionInLandscapeMode.width,
            options.positionInLandscapeMode.height
        )
        rtcOptions.positionInPortraitMode = io.agora.rtc2.video.WatermarkOptions.Rectangle(
            options.positionInPortraitMode.x,
            options.positionInPortraitMode.y,
            options.positionInPortraitMode.width,
            options.positionInPortraitMode.height
        )
        return rtcOptions
    }
}