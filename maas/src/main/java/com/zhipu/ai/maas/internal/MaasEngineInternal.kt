package com.zhipu.ai.maas.internal

import android.util.Log
import android.view.View
import com.zhipu.ai.maas.MaasConstants
import com.zhipu.ai.maas.MaasEngine
import com.zhipu.ai.maas.MaasEngineEventHandler
import com.zhipu.ai.maas.internal.utils.Utils
import com.zhipu.ai.maas.model.MaasEngineConfiguration
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

class MaasEngineInternal : MaasEngine() {
    private var mRtcEngine: RtcEngine? = null
    private var mMaasEngineConfiguration: MaasEngineConfiguration? = null
    private var mEventCallback: MaasEngineEventHandler? = null
    private var mDataStreamId: Int = -1

    override fun initialize(configuration: MaasEngineConfiguration): Int {
        Log.d(MaasConstants.TAG, "initialize configuration:$configuration")
        if (configuration.context == null || configuration.eventHandler == null) {
            Log.e(MaasConstants.TAG, "initialize error: already initialized")
            return MaasConstants.ERROR_INVALID_PARAMS
        }
        mMaasEngineConfiguration = configuration
        mEventCallback = configuration.eventHandler
        try {
            Log.d(MaasConstants.TAG, "RtcEngine version:" + RtcEngine.getSdkVersion())
            val rtcEngineConfig = RtcEngineConfig()
            rtcEngineConfig.mContext = configuration.context
            rtcEngineConfig.mAppId = configuration.appId
            rtcEngineConfig.mChannelProfile =
                Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
            rtcEngineConfig.mEventHandler = object : IRtcEngineEventHandler() {
                override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
                    Log.d(
                        MaasConstants.TAG,
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
                    Log.d(MaasConstants.TAG, "onLeaveChannel")
                    mEventCallback?.onLeaveChannelSuccess()
                }

                override fun onUserJoined(uid: Int, elapsed: Int) {
                    Log.d(MaasConstants.TAG, "onUserJoined uid:$uid elapsed:$elapsed")
                    mEventCallback?.onUserJoined(uid, elapsed)
                }

                override fun onUserOffline(uid: Int, reason: Int) {
                    Log.d(MaasConstants.TAG, "onUserOffline uid:$uid reason:$reason")
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
                        MaasConstants.TAG,
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
                MaasConstants.TAG, "initRtcEngine success"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(
                MaasConstants.TAG, "initRtcEngine error:" + e.message
            )
            return MaasConstants.ERROR_GENERIC
        }
        return MaasConstants.OK
    }

    override fun joinChannel(channelId: String): Int {
        Log.d(MaasConstants.TAG, "joinChannel channelId:$channelId")
        if (mRtcEngine == null) {
            Log.e(MaasConstants.TAG, "joinChannel error: not initialized")
            return MaasConstants.ERROR_NOT_INITIALIZED
        }
        try {
            val ret = mMaasEngineConfiguration?.userId?.let {
                mRtcEngine?.joinChannel(
                    mMaasEngineConfiguration?.rtcToken,
                    channelId,
                    it,
                    object : ChannelMediaOptions() {
                        init {
                            autoSubscribeAudio = true
                            autoSubscribeVideo = true
                            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                        }
                    })
            } ?: MaasConstants.ERROR_INVALID_PARAMS
            Log.d(
                MaasConstants.TAG, "joinChannel ret:$ret"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(
                MaasConstants.TAG, "joinChannel error:" + e.message
            )
            return MaasConstants.ERROR_GENERIC
        }
        return MaasConstants.OK
    }

    override fun leaveChannel(): Int {
        Log.d(MaasConstants.TAG, "leaveChannel")
        if (mRtcEngine == null) {
            Log.e(MaasConstants.TAG, "leaveChannel error: not initialized")
            return MaasConstants.ERROR_NOT_INITIALIZED
        }
        try {
            mRtcEngine?.leaveChannel()
            Log.d(
                MaasConstants.TAG, "leaveChannel"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(
                MaasConstants.TAG, "leaveChannel error:" + e.message
            )
            return MaasConstants.ERROR_GENERIC
        }
        return MaasConstants.OK
    }

    override fun startVideo(
        view: View?,
        renderMode: MaasConstants.RenderMode?,
        position: MaasConstants.VideoModulePosition
    ): Int {
        Log.d(
            MaasConstants.TAG, "startVideo view:$view renderMode:$renderMode position:$position"
        )
        if (mRtcEngine == null) {
            Log.e(MaasConstants.TAG, "startVideo error: not initialized")
            return MaasConstants.ERROR_NOT_INITIALIZED
        }
        var ret = mRtcEngine?.enableVideo()
        Log.d(
            MaasConstants.TAG, "enableVideo ret:$ret"
        )

        ret = mRtcEngine?.updateChannelMediaOptions(object : ChannelMediaOptions() {
            init {
                publishCameraTrack = true
            }
        })
        Log.d(
            MaasConstants.TAG, "updateChannelMediaOptions ret:$ret"
        )


        if (null != view) {
            ret = mRtcEngine?.startPreview()
            Log.d(
                MaasConstants.TAG, "startPreview ret:$ret"
            )

            val local = renderMode?.value?.let { io.agora.rtc2.video.VideoCanvas(view, it, 0) }
                ?: io.agora.rtc2.video.VideoCanvas(view)
            local.position = Utils.getRtcVideoModulePosition(position.value)
            ret = mRtcEngine?.setupLocalVideo(local)
            Log.d(
                MaasConstants.TAG, "setupLocalVideo ret:$ret"
            )
        }

        return if (ret == 0) {
            MaasConstants.OK
        } else {
            MaasConstants.ERROR_GENERIC
        }
    }

    override fun stopVideo(): Int {
        Log.d(MaasConstants.TAG, "stopVideo")
        if (mRtcEngine == null) {
            Log.e(MaasConstants.TAG, "stopVideo error: not initialized")
            return MaasConstants.ERROR_NOT_INITIALIZED
        }
        var ret = mRtcEngine?.stopPreview()
        Log.d(
            MaasConstants.TAG, "stopPreview ret:$ret"
        )
        ret = mRtcEngine?.disableVideo()
        Log.d(
            MaasConstants.TAG, "disableVideo ret:$ret"
        )

        ret = mRtcEngine?.updateChannelMediaOptions(object : ChannelMediaOptions() {
            init {
                publishCameraTrack = false
            }
        })
        Log.d(
            MaasConstants.TAG, "updateChannelMediaOptions ret:$ret"
        )
        return if (ret == 0) {
            MaasConstants.OK
        } else {
            MaasConstants.ERROR_GENERIC
        }
    }

    override fun setVideoEncoderConfiguration(
        width: Int,
        height: Int,
        frameRate: MaasConstants.FrameRate,
        orientationMode: MaasConstants.OrientationMode,
        enableMirrorMode: Boolean
    ): Int {
        Log.d(
            MaasConstants.TAG,
            "setVideoEncoderConfiguration width:$width height:$height frameRate:$frameRate orientationMode:$orientationMode enableMirrorMode:$enableMirrorMode"
        )
        if (mRtcEngine == null) {
            Log.e(MaasConstants.TAG, "setVideoEncoderConfiguration error: not initialized")
            return MaasConstants.ERROR_NOT_INITIALIZED
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
            MaasConstants.OK
        } else {
            MaasConstants.ERROR_GENERIC
        }
    }

    override fun setupRemoteVideo(
        view: View?,
        renderMode: MaasConstants.RenderMode?,
        remoteUid: Int
    ): Int {
        Log.d(
            MaasConstants.TAG,
            "setupRemoteVideo view:$view renderMode:$renderMode remoteUid:$remoteUid"
        )
        if (mRtcEngine == null) {
            Log.e(MaasConstants.TAG, "setupRemoteVideo error: not initialized")
            return MaasConstants.ERROR_NOT_INITIALIZED
        }

        if (null != view) {
            val remote =
                renderMode?.value?.let { io.agora.rtc2.video.VideoCanvas(view, it, remoteUid) }
                    ?: io.agora.rtc2.video.VideoCanvas(view)
            val ret = mRtcEngine?.setupRemoteVideo(remote)
            Log.d(
                MaasConstants.TAG, "setupRemoteVideo ret:$ret"
            )
        }

        return MaasConstants.OK
    }

    override fun switchCamera(): Int {
        Log.d(MaasConstants.TAG, "switchCamera")
        if (mRtcEngine == null) {
            Log.e(MaasConstants.TAG, "switchCamera error: not initialized")
            return MaasConstants.ERROR_NOT_INITIALIZED
        }
        val ret = mRtcEngine?.switchCamera()
        return if (ret == 0) {
            MaasConstants.OK
        } else {
            MaasConstants.ERROR_GENERIC
        }
    }

    override fun addVideoWatermark(watermarkUrl: String, watermarkOptions: WatermarkOptions): Int {
        Log.d(
            MaasConstants.TAG,
            "addVideoWatermark watermarkUrl:$watermarkUrl watermarkOptions:$watermarkOptions"
        )
        if (mRtcEngine == null) {
            Log.e(MaasConstants.TAG, "addVideoWatermark error: not initialized")
            return MaasConstants.ERROR_NOT_INITIALIZED
        }

        val ret = mRtcEngine?.addVideoWatermark(
            watermarkUrl,
            Utils.getRtcWatermarkOptions(watermarkOptions)
        )
        return if (ret == 0) {
            MaasConstants.OK
        } else {
            MaasConstants.ERROR_GENERIC
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
            MaasConstants.TAG,
            "addVideoWatermark data:$data width:$width height:$height format:$format options:$options"
        )
        if (mRtcEngine == null) {
            Log.e(MaasConstants.TAG, "addVideoWatermark error: not initialized")
            return MaasConstants.ERROR_NOT_INITIALIZED
        }

        val ret = mRtcEngine?.addVideoWatermark(
            data,
            width,
            height,
            format,
            Utils.getRtcWatermarkOptions(options)
        )
        return if (ret == 0) {
            MaasConstants.OK
        } else {
            MaasConstants.ERROR_GENERIC
        }
    }

    override fun clearVideoWatermarks(): Int {
        Log.d(MaasConstants.TAG, "clearVideoWatermarks")
        if (mRtcEngine == null) {
            Log.e(MaasConstants.TAG, "clearVideoWatermarks error: not initialized")
            return MaasConstants.ERROR_NOT_INITIALIZED
        }

        val ret = mRtcEngine?.clearVideoWatermarks()
        return if (ret == 0) {
            MaasConstants.OK
        } else {
            MaasConstants.ERROR_GENERIC
        }
    }

    override fun enableAudio(): Int {
        Log.d(MaasConstants.TAG, "enableAudio")
        if (mRtcEngine == null) {
            Log.e(MaasConstants.TAG, "enableAudio error: not initialized")
            return MaasConstants.ERROR_NOT_INITIALIZED
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
            MaasConstants.OK
        } else {
            MaasConstants.ERROR_GENERIC
        }
    }

    override fun disableAudio(): Int {
        Log.d(MaasConstants.TAG, "disableAudio")
        if (mRtcEngine == null) {
            Log.e(MaasConstants.TAG, "disableAudio error: not initialized")
            return MaasConstants.ERROR_NOT_INITIALIZED
        }
        mRtcEngine?.disableAudio()
        val ret = mRtcEngine?.updateChannelMediaOptions(object : ChannelMediaOptions() {
            init {
                publishMicrophoneTrack = false
            }
        })
        return if (ret == 0) {
            MaasConstants.OK
        } else {
            MaasConstants.ERROR_GENERIC
        }
    }

    override fun adjustPlaybackSignalVolume(volume: Int): Int {
        Log.d(MaasConstants.TAG, "adjustPlaybackSignalVolume volume:$volume")
        if (mRtcEngine == null) {
            Log.e(MaasConstants.TAG, "adjustPlaybackSignalVolume error: not initialized")
            return MaasConstants.ERROR_NOT_INITIALIZED
        }
        val ret = mRtcEngine?.adjustPlaybackSignalVolume(volume)
        return if (ret == 0) {
            MaasConstants.OK
        } else {
            MaasConstants.ERROR_GENERIC
        }
    }

    override fun adjustRecordingSignalVolume(volume: Int): Int {
        Log.d(MaasConstants.TAG, "adjustRecordingSignalVolume volume:$volume")
        if (mRtcEngine == null) {
            Log.e(MaasConstants.TAG, "adjustRecordingSignalVolume error: not initialized")
            return MaasConstants.ERROR_NOT_INITIALIZED
        }
        val ret = mRtcEngine?.adjustRecordingSignalVolume(volume)
        return if (ret == 0) {
            MaasConstants.OK
        } else {
            MaasConstants.ERROR_GENERIC
        }
    }

    override fun sendText(text: String): Int {
        Log.d(MaasConstants.TAG, "sendText text:$text")
        if (mRtcEngine == null || mDataStreamId == -1) {
            Log.e(MaasConstants.TAG, "sendText error: not initialized")
            return MaasConstants.ERROR_NOT_INITIALIZED
        }
        val ret = mRtcEngine?.sendStreamMessage(mDataStreamId, text.toByteArray(Charsets.UTF_8))
        return if (ret == 0) {
            MaasConstants.OK
        } else {
            MaasConstants.ERROR_GENERIC
        }
    }

    override fun doDestroy() {
        Log.d(MaasConstants.TAG, "doDestroy")
        RtcEngine.destroy()
        mRtcEngine = null
        mMaasEngineConfiguration = null
        mEventCallback = null
        mDataStreamId = -1
    }

}