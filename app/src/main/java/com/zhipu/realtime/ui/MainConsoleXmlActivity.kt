package com.zhipu.realtime.ui

import android.Manifest
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ToastUtils
import com.zhipu.realtime.BuildConfig
import com.zhipu.realtime.engine.RealtimeEngine
import com.zhipu.realtime.engine.RealtimeEngineEventHandler
import com.zhipu.realtime.constants.RealtimeConstants
import com.zhipu.realtime.databinding.ActivityMainConsoleBinding
import com.zhipu.realtime.model.AudioVolumeInfo
import com.zhipu.realtime.model.Configuration
import com.zhipu.realtime.model.SceneMode
import com.zhipu.realtime.model.VadConfiguration
import com.zhipu.realtime.model.WatermarkOptions
import com.zhipu.realtime.utils.KeyCenterRemote
import com.zhipu.realtime.utils.DataStoreHelper
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.interfaces.OnInputConfirmListener
import com.lxj.xpopup.interfaces.OnSelectListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess


class MainConsoleXmlActivity : AppCompatActivity(), RealtimeEngineEventHandler, OnPopupSaveListener {
    companion object {
        const val TAG: String = RealtimeConstants.TAG + "-MainConsoleXmlActivity"
        const val MY_PERMISSIONS_REQUEST_CODE = 123
    }

    private lateinit var binding: ActivityMainConsoleBinding
    private var mRealtimeEngine: RealtimeEngine? = null
    private var mJoinSuccess = false
    private var mConfiguration: Configuration? = null
    private lateinit var consoleViewManager: ConsoleViewManager

    override fun onCreate(savedInstanceState: Bundle?) {
        /*
        // 禁用夜间模式
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
             AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }*/
        super.onCreate(savedInstanceState)
        binding = ActivityMainConsoleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkPermissions()
        initData()
        initView()
    }

    override fun onDestroy() {
        super.onDestroy()
        consoleViewManager.stopScrolling()
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
        // 事件组件管理器
        consoleViewManager = ConsoleViewManager(binding.eventView)
        consoleViewManager.startScrolling()
        debugViewPrintln("初始化引擎开始")
        // 核心引擎
        mRealtimeEngine = RealtimeEngine.create()
        val configuration = Configuration()
        configuration.context = this
        configuration.eventHandler = this
        configuration.enableConsoleLog = true
        configuration.enableSaveLogToFile = true
        configuration.appId = BuildConfig.APP_ID
        configuration.userId = -1
        configuration.channelId = ""
        configuration.rtcToken = ""
        configuration.enableMultiTurnShortTermMemory = true
        configuration.userName = "test"
        configuration.agentVoiceName = "xiaoyan"
        configuration.input = SceneMode("zh-CN", 16000, 1, 16)
        configuration.output = SceneMode("zh-CN", 16000, 1, 16)
        configuration.vadConfiguration = VadConfiguration(500)
        configuration.noiseEnvironment = RealtimeConstants.NoiseEnvironment.NOISE
        configuration.speechRecognitionCompletenessLevel =
            RealtimeConstants.SpeechRecognitionCompletenessLevel.NORMAL
        var ret = mRealtimeEngine?.initialize(configuration)
        if (ret == 0) {
            Log.d(TAG, "initialize success")
            debugViewPrintln("初始化引擎成功")
        }
        mConfiguration = configuration
    }

    private fun initView() {
        debugViewPrintln("初始化页面组件开始")
        handleOnBackPressed()
        updateUI()

        binding.btnJoin.setOnClickListener {
            lifecycleScope.launch {
                val storedApiKey = withContext(Dispatchers.IO) {
                    DataStoreHelper.getApiKey(applicationContext).firstOrNull() // 假设返回 Flow
                }
                if (storedApiKey.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        XPopup.Builder(this@MainConsoleXmlActivity)
                            .asConfirm(
                                "出错了", "未检测到有效的 API Key，请先设置 API Key。",
                                { /* 点击确认后关闭弹窗，退出协程 */ }
                            ).show()
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    XPopup.Builder(this@MainConsoleXmlActivity).asCenterList(
                        "请选择环境版本",
                        arrayOf("生产环境", "预发布环境"),
                        null,
                        1
                    ) { position, text ->
                        val envToUse = text
                        Toast.makeText(this@MainConsoleXmlActivity, "选择: $text", Toast.LENGTH_SHORT).show()
                        mConfiguration?.let { configuration ->
                            try {
                                KeyCenterRemote.getRtcData(storedApiKey, envToUse, configuration.customSystemPrompt) { result ->
                                    var errorMessage = ""
                                    when (result) {
                                        is KeyCenterRemote.Result.Success -> {
                                            if (result.channelId.isNotEmpty()) {
                                                configuration.userId = result.userId
                                                configuration.channelId = result.channelId
                                                configuration.rtcToken = result.token
                                                mRealtimeEngine?.joinChannel(configuration.channelId)
                                                mRealtimeEngine?.enableAudio()
                                            } else {
                                                errorMessage = "未能获取房间信息"
                                            }
                                        }
                                        is KeyCenterRemote.Result.Failure -> {
                                            errorMessage = result.errorMessage
                                        }
                                    }
                                    if (errorMessage.isNotEmpty()) {
                                        XPopup.Builder(this@MainConsoleXmlActivity).asConfirm(
                                            "出错了", "错误信息: $errorMessage"
                                        ) {}.show()
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "exception when get rtc data", e)
                                XPopup.Builder(this@MainConsoleXmlActivity).asConfirm(
                                    "出错了", "无法获取房间信息，请稍后重试。\n错误信息: ${e.message}"
                                ) {}.show()
                            }
                        }
                    }.show()
                }
            }
        }

        binding.btnLeave.setOnClickListener {
            mRealtimeEngine?.leaveChannel()
        }

        binding.btnSetToken.setOnClickListener {
            binding.btnSetToken.setOnClickListener {
                lifecycleScope.launch {
                    // 读取旧的数据
                    val apiKey = withContext(Dispatchers.IO) {
                        DataStoreHelper.getApiKey(applicationContext).firstOrNull()
                    }
                    val defaultInput = if (!apiKey.isNullOrEmpty()) {
                        apiKey
                    } else {
                        "没有设置APIKEY"
                    }

                    // 切换回主线程显示弹窗
                    withContext(Dispatchers.Main) {
                        XPopup.Builder(this@MainConsoleXmlActivity)
                            .hasStatusBarShadow(false)
                            .hasNavigationBar(false)
                            .isDestroyOnDismiss(true)
                            .autoOpenSoftInput(true)
                            .isDarkTheme(true)
                            .asInputConfirm(
                                "设置APIKEY信息",
                                "访问个人中心，找到APIKEY数据，复制后粘贴到此处。设置后，会保存起来，重新安装后失效。",
                                null,
                                defaultInput,
                                OnInputConfirmListener { input ->
                                    if (!input.isNullOrEmpty() && input != apiKey) {
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            DataStoreHelper.setApiKey(applicationContext, input)
                                            Log.i(TAG, "btnSetToken input: $input")
                                        }
                                    } else {
                                        Log.i(TAG, "No changes detected, skipping save.")
                                    }
                                }
                            )
                            .show()
                    }
                }
            }
        }

        binding.btnStartVideo.setOnClickListener {
            mRealtimeEngine?.startVideo(
                binding.localView,
                RealtimeConstants.RenderMode.HIDDEN
            )

            mRealtimeEngine?.setVideoEncoderConfiguration(
                960,
                480,
                RealtimeConstants.FrameRate.FRAME_RATE_FPS_15,
                RealtimeConstants.OrientationMode.FIXED_LANDSCAPE,
                false
            )
        }

        binding.btnStopVideo.setOnClickListener {
            mRealtimeEngine?.stopVideo()
        }

        binding.btnSwitchCamera.setOnClickListener {
            mRealtimeEngine?.switchCamera()
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

            //  mRealtimeEngine?.addVideoWatermark(
            //      "/assets/agora-logo.png",
            //      watermarkOptions
            //  )

            val rootView = window.decorView.rootView
            val screenBuffer = captureScreenToByteBuffer(rootView)

            mRealtimeEngine?.addVideoWatermark(
                screenBuffer,
                rootView.width,
                rootView.height,
                RealtimeConstants.VideoFormat.VIDEO_PIXEL_RGBA,
                watermarkOptions
            )
        }

        binding.btnClearWatermark.setOnClickListener {
            mRealtimeEngine?.clearVideoWatermarks()
        }

        binding.btnEnableAudio.setOnClickListener {
            mRealtimeEngine?.enableAudio()
        }

        binding.btnDisableAudio.setOnClickListener {
            mRealtimeEngine?.disableAudio()
        }

        binding.btnSendText.setOnClickListener {
            mRealtimeEngine?.sendText("hello world!")
        }

        binding.btnSystemPrompt.setOnClickListener {
            Log.d(TAG, "trigger button btn_system_prompt click")
            // 更新mConfiguration.customSystemPrompt
            // private var mConfiguration: RealtimeEngineConfiguration? = null
            XPopup.Builder(this).autoOpenSoftInput(true).isDestroyOnDismiss(true)
                // .asCustom(SystemPromptEditorPopup2(this, this)).show()
                .asCustom(SystemPromptEditorPopup(this, this)).show()
        }

        binding.btnEnableAins.setOnClickListener {
            Log.d(TAG, "trigger button btn_enable_ains click")
            XPopup.Builder(this)
                .maxWidth(600)
                .maxHeight(800)
                //.isDarkTheme(true)
                .isDestroyOnDismiss(true)
                .asCenterList("请选择AI降噪模式", arrayOf("0：均衡降噪模式", "1：强降噪模式", "2：低延时强降噪模式"),
                    OnSelectListener { position, text ->
                        val mode = text.split("：")[0].toInt()
                        mRealtimeEngine?.setAINSMode(true, mode);
                        ToastUtils.showShort("选择 $text");
                    }
                )
                .show();
        }

        binding.btnDisableAins.setOnClickListener {
            Log.d(TAG, "trigger button btn_disable_ains click")
            mRealtimeEngine?.setAINSMode(false, 0);
            ToastUtils.showShort("关闭降噪模式");
        }
        debugViewPrintln("初始化页面组件完成")
    }

    override fun onPopupSave(systemPrompt: String) {
        mConfiguration?.customSystemPrompt = systemPrompt
        Log.d(TAG, "update system_prompt: $systemPrompt")
    }

    private fun handleOnBackPressed() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val xPopup = XPopup.Builder(this@MainConsoleXmlActivity)
                    .asConfirm("退出", "确认退出程序", {
                        exit()
                    }, {})
                xPopup.show()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun exit() {
        RealtimeEngine.destroy()
        mRealtimeEngine = null
        finishAffinity()
        finish()
        exitProcess(0)
    }

    private fun updateUI() {
        binding.btnJoin.isEnabled = !mJoinSuccess
        binding.btnLeave.isEnabled = mJoinSuccess
        binding.btnSetToken.isEnabled = !mJoinSuccess
        binding.btnStartVideo.isEnabled = mJoinSuccess
        binding.btnStopVideo.isEnabled = mJoinSuccess
        binding.btnSwitchCamera.isEnabled = mJoinSuccess
        binding.btnAddWatermark.isEnabled = mJoinSuccess
        binding.btnClearWatermark.isEnabled = mJoinSuccess
        binding.btnEnableAudio.isEnabled = mJoinSuccess
        binding.btnDisableAudio.isEnabled = mJoinSuccess
        binding.btnSendText.isEnabled = mJoinSuccess
        binding.btnSystemPrompt.isEnabled = !mJoinSuccess
        binding.btnEnableAins.isEnabled = mJoinSuccess
        binding.btnDisableAins.isEnabled = mJoinSuccess
    }

    override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
        Log.d(TAG, "onJoinChannelSuccess channel:$channel uid:$uid elapsed:$elapsed")
        debugViewPrintln("加入房间 channel:$channel uid:$uid elapsed:$elapsed")
        mJoinSuccess = true
        runOnUiThread { updateUI() }
    }

    override fun onLeaveChannelSuccess() {
        Log.d(TAG, "onLeaveChannelSuccess")
        debugViewPrintln("离开房间 再见")
        mJoinSuccess = false
        runOnUiThread { updateUI() }
    }

    override fun onUserJoined(uid: Int, elapsed: Int) {
        Log.d(TAG, "onUserJoined uid:$uid elapsed:$elapsed")
        debugViewPrintln("用户加入 uid:$uid elapsed:$elapsed")
        runOnUiThread {
            mRealtimeEngine?.setupRemoteVideo(
                binding.remoteView,
                RealtimeConstants.RenderMode.FIT,
                uid
            )
        }
    }

    override fun onUserOffline(uid: Int, reason: Int) {
        Log.d(TAG, "onUserOffline uid:$uid reason:$reason")
        debugViewPrintln("用户离线 uid:$uid reason:$reason")
    }

    override fun onAudioVolumeIndication(speakers: ArrayList<AudioVolumeInfo>, totalVolume: Int) {
        // Log.d(TAG, "onAudioVolumeIndication speakers:$speakers totalVolume:$totalVolume")
        // debugViewPrintln("调节音量 speakers:$speakers totalVolume:$totalVolume")
    }

    override fun onStreamMessage(uid: Int, data: ByteArray?) {
        if (data == null) {
            Log.e(TAG, "onStreamMessage uid:$uid data is null.")
            return
        }
        val message = String(data, Charsets.UTF_8)
        Log.d(TAG, "onStreamMessage uid:$uid data:$message")
        val gson = Gson()
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        try {
            val messageMap: Map<String, String> = gson.fromJson(message, mapType)
            val type = messageMap["type"]
            val content = messageMap["content"]
            if (content.isNullOrEmpty()) {
                return
            }
            // 不同类型事件映射到不同颜色
            val typeColorMap = mapOf(
                "conversation.item.input_audio_transcription.completed" to 0xFFFFF176.toInt(), // yellow
                "response.audio_transcript.delta" to 0xFFFFDC00.toInt(), // orange
                "response.text.done" to 0xFFFFDC00.toInt(), // orange
                "response.function_call_arguments.done" to 0xFFACF0F2.toInt(), // azure
                "response.created" to 0xFFBEDB39.toInt(), // green
                "response.done" to 0xFF45BF55.toInt(), // green
                "rtc.backend.reconnect" to 0xFFFF1D23.toInt(), // red
                "rtc.agent.exit" to 0xFFFF1D23.toInt(), // red
                "error" to 0xFFFF1D23.toInt(), // red
            )
            typeColorMap[type]?.let { color ->
                debugViewPrintln(content, color)
            } ?: run {
                Log.e(TAG, "onStreamMessage Unknown type: ${type}")
            }
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "onStreamMessage Failed to parse JSON: ${e.message}")
        }
    }

    override fun debugViewPrintln(content: String, color: Int) {
        val currentTimeMillis = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault(Locale.Category.FORMAT))
        val timeString = dateFormat.format(Date(currentTimeMillis))
        consoleViewManager.appendText("[$timeString] $content", color)
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