# MaasEngine 使用指南

`MaasEngine` 是一个抽象类，提供了一系列用于视频和音频处理的接口。本文档将介绍如何使用该类及其方法。

## 初始化 MaasEngine

在使用 `MaasEngine` 之前，需要先创建并初始化一个 `MaasEngine` 实例。

```kotlin
val maasEngine = MaasEngine.create()
val configuration = MaasEngineConfiguration(/* 配置参数 */)
val result = maasEngine?.initialize(configuration)
if (result == 0) {
    // 初始化成功
} else {
    // 初始化失败
}
```

## 加入和离开频道

```kotlin
val channelId = "your_channel_id"
val joinResult = maasEngine?.joinChannel(channelId)
if (joinResult == 0) {
    // 加入频道成功
} else {
    // 加入频道失败
}

val leaveResult = maasEngine?.leaveChannel()
if (leaveResult == 0) {
    // 离开频道成功
} else {
    // 离开频道失败
}
```

## 视频操作

### 开始和停止视频

```kotlin
val view: View? = /* 视频显示的视图 */
val renderMode: MaasConstants.RenderMode? = /* 渲染模式 */
val position = MaasConstants.VideoModulePosition.VIDEO_MODULE_POSITION_POST_CAPTURER

val startVideoResult = maasEngine?.startVideo(view, renderMode, position)
if (startVideoResult == 0) {
    // 开始视频成功
} else {
    // 开始视频失败
}

val stopVideoResult = maasEngine?.stopVideo()
if (stopVideoResult == 0) {
    // 停止视频成功
} else {
    // 停止视频失败
}
```

### 设置视频编码配置

```kotlin
val width = 1280
val height = 720
val frameRate = MaasConstants.FrameRate.FPS_30
val orientationMode = MaasConstants.OrientationMode.ORIENTATION_MODE_FIXED_LANDSCAPE
val enableMirrorMode = true

val setVideoEncoderConfigResult = maasEngine?.setVideoEncoderConfiguration(
    width, height, frameRate, orientationMode, enableMirrorMode
)
if (setVideoEncoderConfigResult == 0) {
    // 设置视频编码配置成功
} else {
    // 设置视频编码配置失败
}
```

### 设置远程视频

```kotlin
val remoteView: View? = /* 远程视频显示的视图 */
val remoteRenderMode: MaasConstants.RenderMode? = /* 远程渲染模式 */
val remoteUid = 12345

val setupRemoteVideoResult = maasEngine?.setupRemoteVideo(remoteView, remoteRenderMode, remoteUid)
if (setupRemoteVideoResult == 0) {
    // 设置远程视频成功
} else {
    // 设置远程视频失败
}
```

### 切换摄像头

```kotlin
val switchCameraResult = maasEngine?.switchCamera()
if (switchCameraResult == 0) {
    // 切换摄像头成功
} else {
    // 切换摄像头失败
}
```

### 添加和清除视频水印

```kotlin
val watermarkUrl = "https://example.com/watermark.png"
val watermarkOptions = WatermarkOptions(/* 配置参数 */)

val addWatermarkResult = maasEngine?.addVideoWatermark(watermarkUrl, watermarkOptions)
if (addWatermarkResult == 0) {
    // 添加水印成功
} else {
    // 添加水印失败
}

val clearWatermarksResult = maasEngine?.clearVideoWatermarks()
if (clearWatermarksResult == 0) {
    // 清除水印成功
} else {
    // 清除水印失败
}
```

## 音频操作

### 启用和禁用音频

```kotlin
val enableAudioResult = maasEngine?.enableAudio()
if (enableAudioResult == 0) {
    // 启用音频成功
} else {
    // 启用音频失败
}

val disableAudioResult = maasEngine?.disableAudio()
if (disableAudioResult == 0) {
    // 禁用音频成功
} else {
    // 禁用音频失败
}
```

### 调整音量

```kotlin
val playbackVolume = 50
val adjustPlaybackVolumeResult = maasEngine?.adjustPlaybackSignalVolume(playbackVolume)
if (adjustPlaybackVolumeResult == 0) {
    // 调整播放音量成功
} else {
    // 调整播放音量失败
}

val recordingVolume = 50
val adjustRecordingVolumeResult = maasEngine?.adjustRecordingSignalVolume(recordingVolume)
if (adjustRecordingVolumeResult == 0) {
    // 调整录音音量成功
} else {
    // 调整录音音量失败
}
```

## 发送文本消息

```kotlin
val textMessage = "Hello, World!"
val sendTextResult = maasEngine?.sendText(textMessage)
if (sendTextResult == 0) {
    // 发送文本消息成功
} else {
    // 发送文本消息失败
}
```

## 获取 SDK 版本

```kotlin
val sdkVersion = MaasEngine.getSdkVersion()
println("SDK Version: $sdkVersion")
```

## 销毁 MaasEngine 实例

```kotlin
MaasEngine.destroy()
```