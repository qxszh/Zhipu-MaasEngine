package com.zhipu.realtime.engine

import android.util.Log
import android.view.View
import com.zhipu.realtime.constants.RealtimeConstants
import com.zhipu.realtime.model.Configuration
import com.zhipu.realtime.model.WatermarkOptions
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.DataStreamConfig
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoEncoderConfiguration
import io.agora.rtc2.video.VideoEncoderConfiguration.VideoDimensions
import java.nio.ByteBuffer

class RealtimeEngineInternal : RealtimeEngine() {
    private var mRtcEngine: RtcEngine? = null
    private var mConfiguration: Configuration? = null
    private var mEventCallback: RealtimeEngineEventHandler? = null
    private var mDataStreamId: Int = -1

    override fun initialize(configuration: Configuration): Int {
        Log.d(RealtimeConstants.TAG, "initialize configuration:$configuration")
        if (configuration.context == null || configuration.eventHandler == null) {
            Log.e(RealtimeConstants.TAG, "initialize error: already initialized")
            return RealtimeConstants.ERROR_INVALID_PARAMS
        }
        Log.d(RealtimeConstants.TAG, "engine version:" + getEngineVersion())
        mConfiguration = configuration
        mEventCallback = configuration.eventHandler
        try {
            Log.d(RealtimeConstants.TAG, "RtcEngine version:" + RtcEngine.getSdkVersion())
            val rtcEngineConfig = RtcEngineConfig()
            rtcEngineConfig.mContext = configuration.context
            rtcEngineConfig.mAppId = configuration.appId
            rtcEngineConfig.mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
            rtcEngineConfig.mEventHandler = object : IRtcEngineEventHandler() {
                override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
                    Log.d(
                        RealtimeConstants.TAG,
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
                    Log.d(RealtimeConstants.TAG, "onLeaveChannel")
                    mEventCallback?.onLeaveChannelSuccess()
                }

                override fun onUserJoined(uid: Int, elapsed: Int) {
                    Log.d(RealtimeConstants.TAG, "onUserJoined uid:$uid elapsed:$elapsed")
                    mEventCallback?.onUserJoined(uid, elapsed)
                }

                override fun onUserOffline(uid: Int, reason: Int) {
                    Log.d(RealtimeConstants.TAG, "onUserOffline uid:$uid reason:$reason")
                    mEventCallback?.onUserOffline(uid, reason)
                }

                override fun onAudioVolumeIndication(
                    speakers: Array<out AudioVolumeInfo>?,
                    totalVolume: Int
                ) {
//                    Log.d(
//                        RealtimeConstants.TAG,
//                        "onAudioVolumeIndication totalVolume:$totalVolume"
//                    )
                    val allSpeakers = ArrayList<com.zhipu.realtime.model.AudioVolumeInfo>()
                    speakers?.forEach {
                        allSpeakers.add(
                            com.zhipu.realtime.model.AudioVolumeInfo(
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
                        RealtimeConstants.TAG,
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
            mRtcEngine?.setParameters("{\"che.audio.frame_dump\":{\"location\":\"all\",\"action\":\"start\",\"max_size_bytes\":\"100000000\",\"uuid\":\"123456789\", \"duration\": \"150000\"}}");

            mRtcEngine?.setDefaultAudioRoutetoSpeakerphone(true)

            mRtcEngine?.setRecordingAudioFrameParameters(
                16000,
                1,
                Constants.RAW_AUDIO_FRAME_OP_MODE_READ_ONLY,
                320
            )

            Log.d(
                RealtimeConstants.TAG, "initRtcEngine success"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(
                RealtimeConstants.TAG, "initRtcEngine error:" + e.message
            )
            return RealtimeConstants.ERROR_GENERIC
        }
        return RealtimeConstants.OK
    }

    override fun joinChannel(channelId: String): Int {
        Log.d(RealtimeConstants.TAG, "joinChannel channelId:$channelId")
        if (mRtcEngine == null) {
            Log.e(RealtimeConstants.TAG, "joinChannel error: not initialized")
            return RealtimeConstants.ERROR_NOT_INITIALIZED
        }
        try {
            val ret = mConfiguration?.userId?.let {
                mRtcEngine?.joinChannel(
                    mConfiguration?.rtcToken,
                    channelId,
                    it,
                    object : ChannelMediaOptions() {
                        init {
                            autoSubscribeAudio = true
                            autoSubscribeVideo = true
                            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                        }
                    })
            } ?: RealtimeConstants.ERROR_INVALID_PARAMS
            Log.d(
                RealtimeConstants.TAG, "joinChannel ret:$ret"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(
                RealtimeConstants.TAG, "joinChannel error:" + e.message
            )
            return RealtimeConstants.ERROR_GENERIC
        }
        return RealtimeConstants.OK
    }

    override fun leaveChannel(): Int {
        Log.d(RealtimeConstants.TAG, "leaveChannel")
        if (mRtcEngine == null) {
            Log.e(RealtimeConstants.TAG, "leaveChannel error: not initialized")
            return RealtimeConstants.ERROR_NOT_INITIALIZED
        }
        try {
            mRtcEngine?.leaveChannel()
            Log.d(
                RealtimeConstants.TAG, "leaveChannel"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(
                RealtimeConstants.TAG, "leaveChannel error:" + e.message
            )
            return RealtimeConstants.ERROR_GENERIC
        }
        return RealtimeConstants.OK
    }

    override fun startVideo(
        view: View?,
        renderMode: RealtimeConstants.RenderMode?,
        position: RealtimeConstants.VideoModulePosition
    ): Int {
        Log.d(
            RealtimeConstants.TAG, "startVideo view:$view renderMode:$renderMode position:$position"
        )
        if (mRtcEngine == null) {
            Log.e(RealtimeConstants.TAG, "startVideo error: not initialized")
            return RealtimeConstants.ERROR_NOT_INITIALIZED
        }
        var ret = mRtcEngine?.enableVideo()
        Log.d(
            RealtimeConstants.TAG, "enableVideo ret:$ret"
        )

        ret = mRtcEngine?.updateChannelMediaOptions(object : ChannelMediaOptions() {
            init {
                publishCameraTrack = true
            }
        })
        Log.d(
            RealtimeConstants.TAG, "updateChannelMediaOptions ret:$ret"
        )


        if (null != view) {
            ret = mRtcEngine?.startPreview()
            Log.d(
                RealtimeConstants.TAG, "startPreview ret:$ret"
            )

            val local = renderMode?.value?.let { io.agora.rtc2.video.VideoCanvas(view, it, 0) }
                ?: io.agora.rtc2.video.VideoCanvas(view)
            local.position = RealtimeEngineOptionsHelper.getRtcVideoModulePosition(position.value)
            ret = mRtcEngine?.setupLocalVideo(local)
            Log.d(
                RealtimeConstants.TAG, "setupLocalVideo ret:$ret"
            )
        }

        return if (ret == 0) {
            RealtimeConstants.OK
        } else {
            RealtimeConstants.ERROR_GENERIC
        }
    }

    override fun stopVideo(): Int {
        Log.d(RealtimeConstants.TAG, "stopVideo")
        if (mRtcEngine == null) {
            Log.e(RealtimeConstants.TAG, "stopVideo error: not initialized")
            return RealtimeConstants.ERROR_NOT_INITIALIZED
        }
        var ret = mRtcEngine?.stopPreview()
        Log.d(
            RealtimeConstants.TAG, "stopPreview ret:$ret"
        )
        ret = mRtcEngine?.disableVideo()
        Log.d(
            RealtimeConstants.TAG, "disableVideo ret:$ret"
        )

        ret = mRtcEngine?.updateChannelMediaOptions(object : ChannelMediaOptions() {
            init {
                publishCameraTrack = false
            }
        })
        Log.d(
            RealtimeConstants.TAG, "updateChannelMediaOptions ret:$ret"
        )
        return if (ret == 0) {
            RealtimeConstants.OK
        } else {
            RealtimeConstants.ERROR_GENERIC
        }
    }

    override fun setVideoEncoderConfiguration(
        width: Int,
        height: Int,
        frameRate: RealtimeConstants.FrameRate,
        orientationMode: RealtimeConstants.OrientationMode,
        enableMirrorMode: Boolean
    ): Int {
        Log.d(
            RealtimeConstants.TAG,
            "setVideoEncoderConfiguration width:$width height:$height frameRate:$frameRate orientationMode:$orientationMode enableMirrorMode:$enableMirrorMode"
        )
        if (mRtcEngine == null) {
            Log.e(RealtimeConstants.TAG, "setVideoEncoderConfiguration error: not initialized")
            return RealtimeConstants.ERROR_NOT_INITIALIZED
        }
        val ret = mRtcEngine?.setVideoEncoderConfiguration(
            VideoEncoderConfiguration(
                VideoDimensions(width, height),
                RealtimeEngineOptionsHelper.getRtcFrameRate(frameRate.value),
                VideoEncoderConfiguration.STANDARD_BITRATE,
                RealtimeEngineOptionsHelper.getRtcOrientationMode(orientationMode.value),
                if (enableMirrorMode) {
                    VideoEncoderConfiguration.MIRROR_MODE_TYPE.MIRROR_MODE_ENABLED
                } else {
                    VideoEncoderConfiguration.MIRROR_MODE_TYPE.MIRROR_MODE_DISABLED
                }
            )
        )
        return if (ret == 0) {
            RealtimeConstants.OK
        } else {
            RealtimeConstants.ERROR_GENERIC
        }
    }

    override fun setAINSMode(enabled: Boolean, mode: Int): Int {
        Log.d(
            RealtimeConstants.TAG,
            "setAINSMode enabled:$enabled mode:$mode"
        )
        if (mRtcEngine == null) {
            Log.e(RealtimeConstants.TAG, "setAINSMode error: not initialized")
            return RealtimeConstants.ERROR_NOT_INITIALIZED
        }
        val ret = mRtcEngine?.setAINSMode(enabled, mode)
        return if (ret == 0) {
            RealtimeConstants.OK
        } else {
            Log.e(RealtimeConstants.TAG, "setAINSMode error: enabled:$enabled mode:$mode ret:$ret")
            RealtimeConstants.ERROR_GENERIC
        }
    }

    override fun setupRemoteVideo(
        view: View?,
        renderMode: RealtimeConstants.RenderMode?,
        remoteUid: Int
    ): Int {
        Log.d(
            RealtimeConstants.TAG,
            "setupRemoteVideo view:$view renderMode:$renderMode remoteUid:$remoteUid"
        )
        if (mRtcEngine == null) {
            Log.e(RealtimeConstants.TAG, "setupRemoteVideo error: not initialized")
            return RealtimeConstants.ERROR_NOT_INITIALIZED
        }

        if (null != view) {
            val remote =
                renderMode?.value?.let { io.agora.rtc2.video.VideoCanvas(view, it, remoteUid) }
                    ?: io.agora.rtc2.video.VideoCanvas(view)
            val ret = mRtcEngine?.setupRemoteVideo(remote)
            Log.d(
                RealtimeConstants.TAG, "setupRemoteVideo ret:$ret"
            )
        }

        return RealtimeConstants.OK
    }

    override fun switchCamera(): Int {
        Log.d(RealtimeConstants.TAG, "switchCamera")
        if (mRtcEngine == null) {
            Log.e(RealtimeConstants.TAG, "switchCamera error: not initialized")
            return RealtimeConstants.ERROR_NOT_INITIALIZED
        }
        val ret = mRtcEngine?.switchCamera()
        return if (ret == 0) {
            RealtimeConstants.OK
        } else {
            RealtimeConstants.ERROR_GENERIC
        }
    }

    override fun addVideoWatermark(watermarkUrl: String, watermarkOptions: WatermarkOptions): Int {
        Log.d(
            RealtimeConstants.TAG,
            "addVideoWatermark watermarkUrl:$watermarkUrl watermarkOptions:$watermarkOptions"
        )
        if (mRtcEngine == null) {
            Log.e(RealtimeConstants.TAG, "addVideoWatermark error: not initialized")
            return RealtimeConstants.ERROR_NOT_INITIALIZED
        }

        val ret = mRtcEngine?.addVideoWatermark(
            watermarkUrl,
            RealtimeEngineOptionsHelper.getRtcWatermarkOptions(watermarkOptions)
        )
        Log.d(
            RealtimeConstants.TAG,
            "addVideoWatermark ret:$ret"
        )
        return if (ret == 0) {
            RealtimeConstants.OK
        } else {
            RealtimeConstants.ERROR_GENERIC
        }
    }

    override fun addVideoWatermark(
        data: ByteBuffer,
        width: Int,
        height: Int,
        format: RealtimeConstants.VideoFormat,
        options: WatermarkOptions
    ): Int {
        Log.d(
            RealtimeConstants.TAG,
            "addVideoWatermark data:$data width:$width height:$height format:$format options:$options"
        )
        if (mRtcEngine == null) {
            Log.e(RealtimeConstants.TAG, "addVideoWatermark error: not initialized")
            return RealtimeConstants.ERROR_NOT_INITIALIZED
        }
        val ret = mRtcEngine?.addVideoWatermark(
            data,
            width,
            height,
            format.value,
            RealtimeEngineOptionsHelper.getRtcWatermarkOptions(options)
        )
        Log.d(
            RealtimeConstants.TAG,
            "addVideoWatermark ret:$ret"
        )
        return if (ret == 0) {
            RealtimeConstants.OK
        } else {
            RealtimeConstants.ERROR_GENERIC
        }
    }

    override fun clearVideoWatermarks(): Int {
        Log.d(RealtimeConstants.TAG, "clearVideoWatermarks")
        if (mRtcEngine == null) {
            Log.e(RealtimeConstants.TAG, "clearVideoWatermarks error: not initialized")
            return RealtimeConstants.ERROR_NOT_INITIALIZED
        }

        val ret = mRtcEngine?.clearVideoWatermarks()
        return if (ret == 0) {
            RealtimeConstants.OK
        } else {
            RealtimeConstants.ERROR_GENERIC
        }
    }

    override fun enableAudio(): Int {
        Log.d(RealtimeConstants.TAG, "enableAudio")
        if (mRtcEngine == null) {
            Log.e(RealtimeConstants.TAG, "enableAudio error: not initialized")
            return RealtimeConstants.ERROR_NOT_INITIALIZED
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
            RealtimeConstants.OK
        } else {
            RealtimeConstants.ERROR_GENERIC
        }
    }

    override fun disableAudio(): Int {
        Log.d(RealtimeConstants.TAG, "disableAudio")
        if (mRtcEngine == null) {
            Log.e(RealtimeConstants.TAG, "disableAudio error: not initialized")
            return RealtimeConstants.ERROR_NOT_INITIALIZED
        }
        mRtcEngine?.disableAudio()
        val ret = mRtcEngine?.updateChannelMediaOptions(object : ChannelMediaOptions() {
            init {
                publishMicrophoneTrack = false
                // autoSubscribeAudio = true
            }
        })
        return if (ret == 0) {
            RealtimeConstants.OK
        } else {
            RealtimeConstants.ERROR_GENERIC
        }
    }

    override fun adjustPlaybackSignalVolume(volume: Int): Int {
        Log.d(RealtimeConstants.TAG, "adjustPlaybackSignalVolume volume:$volume")
        if (mRtcEngine == null) {
            Log.e(RealtimeConstants.TAG, "adjustPlaybackSignalVolume error: not initialized")
            return RealtimeConstants.ERROR_NOT_INITIALIZED
        }
        val ret = mRtcEngine?.adjustPlaybackSignalVolume(volume)
        return if (ret == 0) {
            RealtimeConstants.OK
        } else {
            RealtimeConstants.ERROR_GENERIC
        }
    }

    override fun adjustRecordingSignalVolume(volume: Int): Int {
        Log.d(RealtimeConstants.TAG, "adjustRecordingSignalVolume volume:$volume")
        if (mRtcEngine == null) {
            Log.e(RealtimeConstants.TAG, "adjustRecordingSignalVolume error: not initialized")
            return RealtimeConstants.ERROR_NOT_INITIALIZED
        }
        val ret = mRtcEngine?.adjustRecordingSignalVolume(volume)
        return if (ret == 0) {
            RealtimeConstants.OK
        } else {
            RealtimeConstants.ERROR_GENERIC
        }
    }

    override fun sendText(text: String): Int {
        Log.d(RealtimeConstants.TAG, "sendText text:$text")
        if (mRtcEngine == null || mDataStreamId == -1) {
            Log.e(RealtimeConstants.TAG, "sendText error: not initialized")
            return RealtimeConstants.ERROR_NOT_INITIALIZED
        }
        val ret = mRtcEngine?.sendStreamMessage(mDataStreamId, text.toByteArray(Charsets.UTF_8))
        return if (ret == 0) {
            RealtimeConstants.OK
        } else {
            RealtimeConstants.ERROR_GENERIC
        }
    }

    override fun doDestroy() {
        Log.d(RealtimeConstants.TAG, "doDestroy")
        RtcEngine.destroy()
        mRtcEngine = null
        mConfiguration = null
        mEventCallback = null
        mDataStreamId = -1
    }

}