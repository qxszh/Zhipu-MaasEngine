package com.zhipu.ai.maas

import android.view.View
import androidx.annotation.Keep
import com.zhipu.ai.maas.internal.MaaSEngineInternal
import com.zhipu.ai.maas.model.MaaSEngineConfiguration
import com.zhipu.ai.maas.model.WatermarkOptions
import java.nio.ByteBuffer

abstract class MaaSEngine {
    abstract fun initialize(configuration: MaaSEngineConfiguration): Int

    abstract fun joinChannel(channelId: String): Int
    abstract fun leaveChannel(): Int
    abstract fun startVideo(
        view: View?,
        renderMode: MaaSConstants.RenderMode?,
        position: MaaSConstants.VideoModulePosition = MaaSConstants.VideoModulePosition.VIDEO_MODULE_POSITION_POST_CAPTURER
    ): Int

    abstract fun stopVideo(): Int
    abstract fun setVideoEncoderConfiguration(
        width: Int,
        height: Int,
        frameRate: MaaSConstants.FrameRate,
        orientationMode: MaaSConstants.OrientationMode,
        enableMirrorMode: Boolean
    ): Int

    abstract fun setupRemoteVideo(
        view: View?,
        renderMode: MaaSConstants.RenderMode?,
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
        private var mInstance: MaaSEngine? = null

        @JvmStatic
        @Synchronized
        @Keep
        fun create(): MaaSEngine? {
            if (mInstance == null) {
                mInstance = MaaSEngineInternal()
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