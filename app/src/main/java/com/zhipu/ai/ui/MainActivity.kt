package com.zhipu.ai.ui

import android.Manifest
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.lxj.xpopup.XPopup
import com.zhipu.ai.BuildConfig
import com.zhipu.ai.constants.Constants
import com.zhipu.ai.databinding.ActivityMainBinding
import com.zhipu.ai.maas.MaaSConstants
import com.zhipu.ai.maas.MaaSEngine
import com.zhipu.ai.maas.MaaSEngineEventHandler
import com.zhipu.ai.maas.model.AudioVolumeInfo
import com.zhipu.ai.maas.model.MaaSEngineConfiguration
import com.zhipu.ai.maas.model.SceneMode
import com.zhipu.ai.maas.model.VadConfiguration
import com.zhipu.ai.maas.model.WatermarkOptions
import com.zhipu.ai.utils.KeyCenter
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.nio.ByteBuffer
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity(), MaaSEngineEventHandler {
    companion object {
        const val TAG: String = Constants.TAG + "-MainActivity"
        const val MY_PERMISSIONS_REQUEST_CODE = 123
    }

    private lateinit var binding: ActivityMainBinding

    private var mMaaSEngine: MaaSEngine? = null

    private var mChannelName = "testAga"
    private var mJoinSuccess = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkPermissions()
        initData()
        initView()
    }

    private fun checkPermissions() {
        val permissions =
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
        if (EasyPermissions.hasPermissions(this, *permissions)) {
            // 已经获取到权限，执行相应的操作
            Log.d(TAG, "granted permission")
        } else {
            Log.i(TAG, "requestPermissions")
            EasyPermissions.requestPermissions(
                this,
                "需要录音权限",
                MY_PERMISSIONS_REQUEST_CODE,
                *permissions
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        // 权限被授予，执行相应的操作
        Log.d(TAG, "onPermissionsGranted requestCode:$requestCode perms:$perms")
    }

    fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Log.d(TAG, "onPermissionsDenied requestCode:$requestCode perms:$perms")
        // 权限被拒绝，显示一个提示信息
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            // 如果权限被永久拒绝，可以显示一个对话框引导用户去应用设置页面手动授权
            AppSettingsDialog.Builder(this).build().show()
        }
    }

    private fun initData() {
        mMaaSEngine = MaaSEngine.create()
        val configuration = MaaSEngineConfiguration()
        configuration.context = this
        configuration.eventHandler = this
        configuration.enableConsoleLog = true
        configuration.enableSaveLogToFile = true
        configuration.appId = BuildConfig.APP_ID
        configuration.userId = KeyCenter.getUid()
        configuration.rtcToken = KeyCenter.getRtcToken(mChannelName, KeyCenter.getUid())
        configuration.enableMultiTurnShortTermMemory = true
        configuration.userName = "test"
        configuration.agentVoiceName = "xiaoyan"
        configuration.input = SceneMode("zh-CN", 16000, 1, 16)
        configuration.output = SceneMode("zh-CN", 16000, 1, 16)
        configuration.vadConfiguration = VadConfiguration(500)
        configuration.noiseEnvironment = MaaSConstants.NoiseEnvironment.NOISE
        configuration.speechRecognitionCompletenessLevel =
            MaaSConstants.SpeechRecognitionCompletenessLevel.NORMAL
        var ret = mMaaSEngine?.initialize(configuration)
        if (ret == 0) {
            Log.d(TAG, "initialize success")
        }
    }

    private fun initView() {
        handleOnBackPressed()
        updateUI()

        binding.btnJoin.setOnClickListener {
            mMaaSEngine?.joinChannel(mChannelName)
        }

        binding.btnLeave.setOnClickListener {
            mMaaSEngine?.leaveChannel()
        }

        binding.btnStartVideo.setOnClickListener {
            mMaaSEngine?.startVideo(
                binding.localView,
                MaaSConstants.RenderMode.HIDDEN
            )

            mMaaSEngine?.setVideoEncoderConfiguration(
                640,
                480,
                MaaSConstants.FrameRate.FRAME_RATE_FPS_15,
                MaaSConstants.OrientationMode.FIXED_LANDSCAPE,
                false
            )
        }

        binding.btnStopVideo.setOnClickListener {
            mMaaSEngine?.stopVideo()
        }

        binding.btnSwitchCamera.setOnClickListener {
            mMaaSEngine?.switchCamera()
        }

        binding.btnAddWatermark.setOnClickListener {
            val watermarkOptions = WatermarkOptions()
            val width = 200
            val height = 200
            watermarkOptions.positionInPortraitMode =
                WatermarkOptions.Rectangle(0, 0, width, height)
            watermarkOptions.positionInLandscapeMode =
                WatermarkOptions.Rectangle(0, 0, width, height)
            watermarkOptions.visibleInPreview = true

//            mMaaSEngine?.addVideoWatermark(
//                "/assets/agora-logo.png",
//                watermarkOptions
//            )

            val rootView = window.decorView.rootView
            val screenBuffer = captureScreenToByteBuffer(rootView)

            mMaaSEngine?.addVideoWatermark(
                screenBuffer,
                rootView.width,
                rootView.height,
                MaaSConstants.VideoFormat.VIDEO_PIXEL_RGBA,
                watermarkOptions
            )
        }

        binding.btnClearWatermark.setOnClickListener {
            mMaaSEngine?.clearVideoWatermarks()
        }

        binding.btnEnableAudio.setOnClickListener {
            mMaaSEngine?.enableAudio()
        }

        binding.btnDisableAudio.setOnClickListener {
            mMaaSEngine?.disableAudio()
        }

        binding.btnSendText.setOnClickListener {
            mMaaSEngine?.sendText("hello world!")
        }
    }

    private fun handleOnBackPressed() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val xPopup = XPopup.Builder(this@MainActivity)
                    .asConfirm("退出", "确认退出程序", {
                        exit()
                    }, {})
                xPopup.show()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun exit() {
        MaaSEngine.destroy()
        mMaaSEngine = null
        finishAffinity()
        finish()
        exitProcess(0)
    }

    private fun updateUI() {
        binding.btnJoin.isEnabled = !mJoinSuccess
        binding.btnLeave.isEnabled = mJoinSuccess
        binding.btnStartVideo.isEnabled = mJoinSuccess
        binding.btnStopVideo.isEnabled = mJoinSuccess
        binding.btnSwitchCamera.isEnabled = mJoinSuccess
        binding.btnAddWatermark.isEnabled = mJoinSuccess
        binding.btnClearWatermark.isEnabled = mJoinSuccess
        binding.btnEnableAudio.isEnabled = mJoinSuccess
        binding.btnDisableAudio.isEnabled = mJoinSuccess
        binding.btnSendText.isEnabled = mJoinSuccess
    }

    override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
        Log.d(TAG, "onJoinChannelSuccess channel:$channel uid:$uid elapsed:$elapsed")
        mJoinSuccess = true
        runOnUiThread { updateUI() }
    }

    override fun onLeaveChannelSuccess() {
        Log.d(TAG, "onLeaveChannelSuccess")
        mJoinSuccess = false
        runOnUiThread { updateUI() }
    }

    override fun onUserJoined(uid: Int, elapsed: Int) {
        Log.d(TAG, "onUserJoined uid:$uid elapsed:$elapsed")
        runOnUiThread {
            mMaaSEngine?.setupRemoteVideo(
                binding.remoteView,
                MaaSConstants.RenderMode.FIT,
                uid
            )
        }
    }

    override fun onUserOffline(uid: Int, reason: Int) {
        Log.d(TAG, "onUserOffline uid:$uid reason:$reason")
    }

    override fun onAudioVolumeIndication(speakers: ArrayList<AudioVolumeInfo>, totalVolume: Int) {
        //Log.d(TAG, "onAudioVolumeIndication speakers:$speakers totalVolume:$totalVolume")
    }

    override fun onStreamMessage(uid: Int, data: ByteArray?) {
        Log.d(TAG, "onStreamMessage uid:$uid data:${String(data!!, Charsets.UTF_8)}")
    }

    private fun captureScreenToByteBuffer(view: View): ByteBuffer {
        // 创建一个与视图大小相同的 Bitmap
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)

        // 将视图绘制到 Bitmap
        view.draw(android.graphics.Canvas(bitmap))

        // 计算需要的字节数
        val bytes = bitmap.byteCount

        // 创建一个直接分配的 ByteBuffer
        val buffer = ByteBuffer.allocateDirect(bytes)

        // 将 Bitmap 像素复制到 ByteBuffer
        bitmap.copyPixelsToBuffer(buffer)

        // 重置 buffer 位置
        buffer.rewind()

        // 回收 Bitmap 以释放内存
        bitmap.recycle()

        return buffer
    }
}