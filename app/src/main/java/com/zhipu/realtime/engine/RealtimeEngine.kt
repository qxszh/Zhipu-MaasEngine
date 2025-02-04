package com.zhipu.realtime.engine

import android.view.View
import androidx.annotation.Keep
import com.zhipu.realtime.BuildConfig
import com.zhipu.realtime.constants.RealtimeConstants
import com.zhipu.realtime.model.Configuration
import com.zhipu.realtime.model.WatermarkOptions
import java.nio.ByteBuffer


abstract class RealtimeEngine {
    abstract fun initialize(configuration: Configuration): Int

    abstract fun joinChannel(channelId: String): Int
    abstract fun leaveChannel(): Int
    abstract fun startVideo(
        view: View?,
        renderMode: RealtimeConstants.RenderMode?,
        position: RealtimeConstants.VideoModulePosition = RealtimeConstants.VideoModulePosition.VIDEO_MODULE_POSITION_POST_CAPTURER
    ): Int

    abstract fun stopVideo(): Int
    abstract fun setVideoEncoderConfiguration(
        width: Int,
        height: Int,
        frameRate: RealtimeConstants.FrameRate,
        orientationMode: RealtimeConstants.OrientationMode,
        enableMirrorMode: Boolean
    ): Int

    abstract fun setAINSMode(
        enabled: Boolean,
        mode: Int,
    ): Int

    abstract fun setupRemoteVideo(
        view: View?,
        renderMode: RealtimeConstants.RenderMode?,
        remoteUid: Int
    ): Int

    abstract fun switchCamera(): Int

    abstract fun addVideoWatermark(watermarkUrl: String, watermarkOptions: WatermarkOptions): Int

    abstract fun addVideoWatermark(
        data: ByteBuffer,
        width: Int,
        height: Int,
        format: RealtimeConstants.VideoFormat,
        options: WatermarkOptions
    ): Int

    abstract fun clearVideoWatermarks(): Int

    abstract fun enableAudio(): Int

    abstract fun disableAudio(): Int

    abstract fun adjustPlaybackSignalVolume(volume: Int): Int

    abstract fun adjustRecordingSignalVolume(volume: Int): Int

    abstract fun sendText(text: String): Int

    protected abstract fun doDestroy()

    companion object {
        @JvmStatic
        private var mInstance: RealtimeEngine? = null

        @JvmStatic
        @Synchronized
        @Keep
        fun create(): RealtimeEngine? {
            if (mInstance == null) {
                mInstance = RealtimeEngineInternal()
            }
            return mInstance
        }

        @JvmStatic
        @Synchronized
        @Keep
        fun destroy() {
            mInstance?.doDestroy()
            mInstance = null
        }

        @JvmStatic
        @Synchronized
        @Keep
        fun getEngineVersion(): String {
            return BuildConfig.VERSION_NAME
        }
    }
}