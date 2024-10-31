package com.zhipu.ai.maas

import android.view.View
import androidx.annotation.Keep
import com.zhipu.ai.maas.internal.MaasEngineInternal
import com.zhipu.ai.maas.model.MaasEngineConfiguration
import com.zhipu.ai.maas.model.WatermarkOptions
import java.nio.ByteBuffer

abstract class MaasEngine {
    abstract fun initialize(configuration: MaasEngineConfiguration): Int

    abstract fun joinChannel(channelId: String): Int
    abstract fun leaveChannel(): Int
    abstract fun startVideo(
        view: View?,
        renderMode: MaasConstants.RenderMode?,
        position: MaasConstants.VideoModulePosition = MaasConstants.VideoModulePosition.VIDEO_MODULE_POSITION_POST_CAPTURER
    ): Int

    abstract fun stopVideo(): Int
    abstract fun setVideoEncoderConfiguration(
        width: Int,
        height: Int,
        frameRate: MaasConstants.FrameRate,
        orientationMode: MaasConstants.OrientationMode,
        enableMirrorMode: Boolean
    ): Int

    abstract fun setupRemoteVideo(
        view: View?,
        renderMode: MaasConstants.RenderMode?,
        remoteUid: Int
    ): Int

    abstract fun switchCamera(): Int

    abstract fun addVideoWatermark(watermarkUrl: String, watermarkOptions: WatermarkOptions): Int

    abstract fun addVideoWatermark(
        data: ByteBuffer,
        width: Int,
        height: Int,
        format: Int,
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
        private var mInstance: MaasEngine? = null

        @JvmStatic
        @Synchronized
        @Keep
        fun create(): MaasEngine? {
            if (mInstance == null) {
                mInstance = MaasEngineInternal()
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
        fun getSdkVersion(): String {
            return BuildConfig.VERSION_NAME
        }
    }
}