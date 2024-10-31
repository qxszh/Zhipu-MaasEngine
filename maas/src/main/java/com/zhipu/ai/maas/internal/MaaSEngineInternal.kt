package com.zhipu.ai.maas.internal

import android.util.Log
import android.view.View
import com.zhipu.ai.maas.MaaSConstants
import com.zhipu.ai.maas.MaaSEngine
import com.zhipu.ai.maas.MaaSEngineEventHandler
import com.zhipu.ai.maas.internal.utils.Utils
import com.zhipu.ai.maas.model.MaaSEngineConfiguration
import com.zhipu.ai.maas.model.WatermarkOptions
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.DataStreamConfig
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoEncoderConfiguration
import io.agora.rtc2.video.VideoEncoderConfiguration.VideoDimensions
import java.nio.ByteBuffer

class MaaSEngineInternal : MaaSEngine() {
    private var mRtcEngine: RtcEngine? = null
    private var mMaaSEngineConfiguration: MaaSEngineConfiguration? = null
    private var mEventCallback: MaaSEngineEventHandler? = null
    private var mDataStreamId: Int = -1

    override fun initialize(configuration: MaaSEngineConfiguration): Int {
        Log.d(MaaSConstants.TAG, "initialize configuration:$configuration")
        if (configuration.context == null || configuration.eventHandler == null) {
            Log.e(MaaSConstants.TAG, "initialize error: already initialized")
            return MaaSConstants.ERROR_INVALID_PARAMS
        }
        Log.d(MaaSConstants.TAG, "maas version:" + getSdkVersion())
        mMaaSEngineConfiguration = configuration
        mEventCallback = configuration.eventHandler
        try {
            Log.d(MaaSConstants.TAG, "RtcEngine version:" + RtcEngine.getSdkVersion())
            val rtcEngineConfig = RtcEngineConfig()
            rtcEngineConfig.mContext = configuration.context
            rtcEngineConfig.mAppId = configuration.appId
            rtcEngineConfig.mChannelProfile =
                Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
            rtcEngineConfig.mEventHandler = object : IRtcEngineEventHandler() {
                override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
                    Log.d(
                        MaaSConstants.TAG,
                        "onJoinChannelSuccess channel:$channel uid:$uid elapsed:$elapsed"
                    )
                    if (-1 == mDataStreamId) {
                        val cfg = DataStreamConfig()
                        cfg.syncWithAudio = false
                        cfg.ordered = true
                        mDataStreamId = mRtcEngine?.createDataStream(cfg) ?: -1
                    }
                    mEventCallback?.onJoinChannelSuccess(channel, uid, elapsed)
                }

                override fun onLeaveChannel(stats: RtcStats) {
                    Log.d(MaaSConstants.TAG, "onLeaveChannel")
                    mEventCallback?.onLeaveChannelSuccess()
                }

                override fun onUserJoined(uid: Int, elapsed: Int) {
                    Log.d(MaaSConstants.TAG, "onUserJoined uid:$uid elapsed:$elapsed")
                    mEventCallback?.onUserJoined(uid, elapsed)
                }

                override fun onUserOffline(uid: Int, reason: Int) {
                    Log.d(MaaSConstants.TAG, "onUserOffline uid:$uid reason:$reason")
                    mEventCallback?.onUserOffline(uid, reason)
                }

                override fun onAudioVolumeIndication(
                    speakers: Array<out AudioVolumeInfo>?,
                    totalVolume: Int
                ) {
//                    Log.d(
//                        MaasConstants.TAG,
//                        "onAudioVolumeIndication totalVolume:$totalVolume"
//                    )
                    val allSpeakers = ArrayList<com.zhipu.ai.maas.model.AudioVolumeInfo>()
                    speakers?.forEach {
                        allSpeakers.add(
                            com.zhipu.ai.maas.model.AudioVolumeInfo(
                                it.uid,
                                it.volume
                            )
                        )
                    }
                    mEventCallback?.onAudioVolumeIndication(allSpeakers, totalVolume)
                }

                override fun onStreamMessage(uid: Int, streamId: Int, data: ByteArray?) {
                    super.onStreamMessage(uid, streamId, data)
                    Log.d(
                        MaaSConstants.TAG,
                        "onStreamMessage uid:$uid streamId:$streamId data:${data?.toString()}"
                    )
                    mEventCallback?.onStreamMessage(uid, data)
                }
            }
            rtcEngineConfig.mAudioScenario = Constants.AUDIO_SCENARIO_CHORUS
            mRtcEngine = RtcEngine.create(rtcEngineConfig)

            mRtcEngine?.setAudioProfile(
                Constants.AUDIO_SCENARIO_CHORUS,
                Constants.AUDIO_SCENARIO_GAME_STREAMING
            )

            mRtcEngine?.setParameters("{\"rtc.enable_debug_log\":true}")

            mRtcEngine?.setDefaultAudioRoutetoSpeakerphone(true)

            mRtcEngine?.setRecordingAudioFrameParameters(
                16000,
                1,
                Constants.RAW_AUDIO_FRAME_OP_MODE_READ_ONLY,
                640
            )

            Log.d(
                MaaSConstants.TAG, "initRtcEngine success"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(
                MaaSConstants.TAG, "initRtcEngine error:" + e.message
            )
            return MaaSConstants.ERROR_GENERIC
        }
        return MaaSConstants.OK
    }

    override fun joinChannel(channelId: String): Int {
        Log.d(MaaSConstants.TAG, "joinChannel channelId:$channelId")
        if (mRtcEngine == null) {
            Log.e(MaaSConstants.TAG, "joinChannel error: not initialized")
            return MaaSConstants.ERROR_NOT_INITIALIZED
        }
        try {
            val ret = mMaaSEngineConfiguration?.userId?.let {
                mRtcEngine?.joinChannel(
                    mMaaSEngineConfiguration?.rtcToken,
                    channelId,
                    it,
                    object : ChannelMediaOptions() {
                        init {
                            autoSubscribeAudio = true
                            autoSubscribeVideo = true
                            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                        }
                    })
            } ?: MaaSConstants.ERROR_INVALID_PARAMS
            Log.d(
                MaaSConstants.TAG, "joinChannel ret:$ret"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(
                MaaSConstants.TAG, "joinChannel error:" + e.message
            )
            return MaaSConstants.ERROR_GENERIC
        }
        return MaaSConstants.OK
    }

    override fun leaveChannel(): Int {
        Log.d(MaaSConstants.TAG, "leaveChannel")
        if (mRtcEngine == null) {
            Log.e(MaaSConstants.TAG, "leaveChannel error: not initialized")
            return MaaSConstants.ERROR_NOT_INITIALIZED
        }
        try {
            mRtcEngine?.leaveChannel()
            Log.d(
                MaaSConstants.TAG, "leaveChannel"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(
                MaaSConstants.TAG, "leaveChannel error:" + e.message
            )
            return MaaSConstants.ERROR_GENERIC
        }
        return MaaSConstants.OK
    }

    override fun startVideo(
        view: View?,
        renderMode: MaaSConstants.RenderMode?,
        position: MaaSConstants.VideoModulePosition
    ): Int {
        Log.d(
            MaaSConstants.TAG, "startVideo view:$view renderMode:$renderMode position:$position"
        )
        if (mRtcEngine == null) {
            Log.e(MaaSConstants.TAG, "startVideo error: not initialized")
            return MaaSConstants.ERROR_NOT_INITIALIZED
        }
        var ret = mRtcEngine?.enableVideo()
        Log.d(
            MaaSConstants.TAG, "enableVideo ret:$ret"
        )

        ret = mRtcEngine?.updateChannelMediaOptions(object : ChannelMediaOptions() {
            init {
                publishCameraTrack = true
            }
        })
        Log.d(
            MaaSConstants.TAG, "updateChannelMediaOptions ret:$ret"
        )


        if (null != view) {
            ret = mRtcEngine?.startPreview()
            Log.d(
                MaaSConstants.TAG, "startPreview ret:$ret"
            )

            val local = renderMode?.value?.let { io.agora.rtc2.video.VideoCanvas(view, it, 0) }
                ?: io.agora.rtc2.video.VideoCanvas(view)
            local.position = Utils.getRtcVideoModulePosition(position.value)
            ret = mRtcEngine?.setupLocalVideo(local)
            Log.d(
                MaaSConstants.TAG, "setupLocalVideo ret:$ret"
            )
        }

        return if (ret == 0) {
            MaaSConstants.OK
        } else {
            MaaSConstants.ERROR_GENERIC
        }
    }

    override fun stopVideo(): Int {
        Log.d(MaaSConstants.TAG, "stopVideo")
        if (mRtcEngine == null) {
            Log.e(MaaSConstants.TAG, "stopVideo error: not initialized")
            return MaaSConstants.ERROR_NOT_INITIALIZED
        }
        var ret = mRtcEngine?.stopPreview()
        Log.d(
            MaaSConstants.TAG, "stopPreview ret:$ret"
        )
        ret = mRtcEngine?.disableVideo()
        Log.d(
            MaaSConstants.TAG, "disableVideo ret:$ret"
        )

        ret = mRtcEngine?.updateChannelMediaOptions(object : ChannelMediaOptions() {
            init {
                publishCameraTrack = false
            }
        })
        Log.d(
            MaaSConstants.TAG, "updateChannelMediaOptions ret:$ret"
        )
        return if (ret == 0) {
            MaaSConstants.OK
        } else {
            MaaSConstants.ERROR_GENERIC
        }
    }

    override fun setVideoEncoderConfiguration(
        width: Int,
        height: Int,
        frameRate: MaaSConstants.FrameRate,
        orientationMode: MaaSConstants.OrientationMode,
        enableMirrorMode: Boolean
    ): Int {
        Log.d(
            MaaSConstants.TAG,
            "setVideoEncoderConfiguration width:$width height:$height frameRate:$frameRate orientationMode:$orientationMode enableMirrorMode:$enableMirrorMode"
        )
        if (mRtcEngine == null) {
            Log.e(MaaSConstants.TAG, "setVideoEncoderConfiguration error: not initialized")
            return MaaSConstants.ERROR_NOT_INITIALIZED
        }
        val ret = mRtcEngine?.setVideoEncoderConfiguration(
            VideoEncoderConfiguration(
                VideoDimensions(width, height),
                Utils.getRtcFrameRate(frameRate.value),
                VideoEncoderConfiguration.STANDARD_BITRATE,
                Utils.getRtcOrientationMode(orientationMode.value),
                if (enableMirrorMode) {
                    VideoEncoderConfiguration.MIRROR_MODE_TYPE.MIRROR_MODE_ENABLED
                } else {
                    VideoEncoderConfiguration.MIRROR_MODE_TYPE.MIRROR_MODE_DISABLED
                }
            )
        )
        return if (ret == 0) {
            MaaSConstants.OK
        } else {
            MaaSConstants.ERROR_GENERIC
        }
    }

    override fun setupRemoteVideo(
        view: View?,
        renderMode: MaaSConstants.RenderMode?,
        remoteUid: Int
    ): Int {
        Log.d(
            MaaSConstants.TAG,
            "setupRemoteVideo view:$view renderMode:$renderMode remoteUid:$remoteUid"
        )
        if (mRtcEngine == null) {
            Log.e(MaaSConstants.TAG, "setupRemoteVideo error: not initialized")
            return MaaSConstants.ERROR_NOT_INITIALIZED
        }

        if (null != view) {
            val remote =
                renderMode?.value?.let { io.agora.rtc2.video.VideoCanvas(view, it, remoteUid) }
                    ?: io.agora.rtc2.video.VideoCanvas(view)
            val ret = mRtcEngine?.setupRemoteVideo(remote)
            Log.d(
                MaaSConstants.TAG, "setupRemoteVideo ret:$ret"
            )
        }

        return MaaSConstants.OK
    }

    override fun switchCamera(): Int {
        Log.d(MaaSConstants.TAG, "switchCamera")
        if (mRtcEngine == null) {
            Log.e(MaaSConstants.TAG, "switchCamera error: not initialized")
            return MaaSConstants.ERROR_NOT_INITIALIZED
        }
        val ret = mRtcEngine?.switchCamera()
        return if (ret == 0) {
            MaaSConstants.OK
        } else {
            MaaSConstants.ERROR_GENERIC
        }
    }

    override fun addVideoWatermark(watermarkUrl: String, watermarkOptions: WatermarkOptions): Int {
        Log.d(
            MaaSConstants.TAG,
            "addVideoWatermark watermarkUrl:$watermarkUrl watermarkOptions:$watermarkOptions"
        )
        if (mRtcEngine == null) {
            Log.e(MaaSConstants.TAG, "addVideoWatermark error: not initialized")
            return MaaSConstants.ERROR_NOT_INITIALIZED
        }

        val ret = mRtcEngine?.addVideoWatermark(
            watermarkUrl,
            Utils.getRtcWatermarkOptions(watermarkOptions)
        )
        return if (ret == 0) {
            MaaSConstants.OK
        } else {
            MaaSConstants.ERROR_GENERIC
        }
    }

    override fun addVideoWatermark(
        data: ByteBuffer,
        width: Int,
        height: Int,
        format: Int,
        options: WatermarkOptions
    ): Int {
        Log.d(
            MaaSConstants.TAG,
            "addVideoWatermark data:$data width:$width height:$height format:$format options:$options"
        )
        if (mRtcEngine == null) {
            Log.e(MaaSConstants.TAG, "addVideoWatermark error: not initialized")
            return MaaSConstants.ERROR_NOT_INITIALIZED
        }

        val ret = mRtcEngine?.addVideoWatermark(
            data,
            width,
            height,
            format,
            Utils.getRtcWatermarkOptions(options)
        )
        return if (ret == 0) {
            MaaSConstants.OK
        } else {
            MaaSConstants.ERROR_GENERIC
        }
    }

    override fun clearVideoWatermarks(): Int {
        Log.d(MaaSConstants.TAG, "clearVideoWatermarks")
        if (mRtcEngine == null) {
            Log.e(MaaSConstants.TAG, "clearVideoWatermarks error: not initialized")
            return MaaSConstants.ERROR_NOT_INITIALIZED
        }

        val ret = mRtcEngine?.clearVideoWatermarks()
        return if (ret == 0) {
            MaaSConstants.OK
        } else {
            MaaSConstants.ERROR_GENERIC
        }
    }

    override fun enableAudio(): Int {
        Log.d(MaaSConstants.TAG, "enableAudio")
        if (mRtcEngine == null) {
            Log.e(MaaSConstants.TAG, "enableAudio error: not initialized")
            return MaaSConstants.ERROR_NOT_INITIALIZED
        }
        mRtcEngine?.enableAudio()
        val ret = mRtcEngine?.updateChannelMediaOptions(object : ChannelMediaOptions() {
            init {
                publishMicrophoneTrack = true
            }
        })
        //min 50ms
        mRtcEngine?.enableAudioVolumeIndication(
            50,
            3,
            true
        )
        return if (ret == 0) {
            MaaSConstants.OK
        } else {
            MaaSConstants.ERROR_GENERIC
        }
    }

    override fun disableAudio(): Int {
        Log.d(MaaSConstants.TAG, "disableAudio")
        if (mRtcEngine == null) {
            Log.e(MaaSConstants.TAG, "disableAudio error: not initialized")
            return MaaSConstants.ERROR_NOT_INITIALIZED
        }
        mRtcEngine?.disableAudio()
        val ret = mRtcEngine?.updateChannelMediaOptions(object : ChannelMediaOptions() {
            init {
                publishMicrophoneTrack = false
            }
        })
        return if (ret == 0) {
            MaaSConstants.OK
        } else {
            MaaSConstants.ERROR_GENERIC
        }
    }

    override fun adjustPlaybackSignalVolume(volume: Int): Int {
        Log.d(MaaSConstants.TAG, "adjustPlaybackSignalVolume volume:$volume")
        if (mRtcEngine == null) {
            Log.e(MaaSConstants.TAG, "adjustPlaybackSignalVolume error: not initialized")
            return MaaSConstants.ERROR_NOT_INITIALIZED
        }
        val ret = mRtcEngine?.adjustPlaybackSignalVolume(volume)
        return if (ret == 0) {
            MaaSConstants.OK
        } else {
            MaaSConstants.ERROR_GENERIC
        }
    }

    override fun adjustRecordingSignalVolume(volume: Int): Int {
        Log.d(MaaSConstants.TAG, "adjustRecordingSignalVolume volume:$volume")
        if (mRtcEngine == null) {
            Log.e(MaaSConstants.TAG, "adjustRecordingSignalVolume error: not initialized")
            return MaaSConstants.ERROR_NOT_INITIALIZED
        }
        val ret = mRtcEngine?.adjustRecordingSignalVolume(volume)
        return if (ret == 0) {
            MaaSConstants.OK
        } else {
            MaaSConstants.ERROR_GENERIC
        }
    }

    override fun sendText(text: String): Int {
        Log.d(MaaSConstants.TAG, "sendText text:$text")
        if (mRtcEngine == null || mDataStreamId == -1) {
            Log.e(MaaSConstants.TAG, "sendText error: not initialized")
            return MaaSConstants.ERROR_NOT_INITIALIZED
        }
        val ret = mRtcEngine?.sendStreamMessage(mDataStreamId, text.toByteArray(Charsets.UTF_8))
        return if (ret == 0) {
            MaaSConstants.OK
        } else {
            MaaSConstants.ERROR_GENERIC
        }
    }

    override fun doDestroy() {
        Log.d(MaaSConstants.TAG, "doDestroy")
        RtcEngine.destroy()
        mRtcEngine = null
        mMaaSEngineConfiguration = null
        mEventCallback = null
        mDataStreamId = -1
    }

}