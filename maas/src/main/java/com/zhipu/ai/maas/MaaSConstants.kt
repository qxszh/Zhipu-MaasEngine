package com.zhipu.ai.maas

class MaaSConstants {
    companion object {
        const val TAG = "MaaSEngine"

        const val ERROR_GENERIC = -1
        const val OK = 0
        const val ERROR_INVALID_PARAMS = 1
        const val ERROR_NOT_INITIALIZED = 2

    }


    enum class NoiseEnvironment(val value: Int) {
        QUIET(0),
        NORMAL(1),
        NOISE(2);

        companion object {
            fun getNoiseEnvironment(noiseEnvironmentValue: Int): NoiseEnvironment? {
                for (noiseEnvironment in entries) {
                    if (noiseEnvironment.value == noiseEnvironmentValue) {
                        return noiseEnvironment
                    }
                }
                return null
            }
        }
    }

    enum class SpeechRecognitionCompletenessLevel(val value: Int) {
        LOW(0),
        NORMAL(1),
        HIGH(2);

        companion object {
            fun getNoiseEnvironment(speechRecognitionCompleteLevelValue: Int): SpeechRecognitionCompletenessLevel? {
                for (speechRecognitionCompleteLevel in entries) {
                    if (speechRecognitionCompleteLevel.value == speechRecognitionCompleteLevelValue) {
                        return speechRecognitionCompleteLevel
                    }
                }
                return null
            }
        }
    }

    enum class RenderMode(val value: Int) {
        HIDDEN(1),
        FIT(2),
        ADAPTIVE(3);

        companion object {
            fun getRenderMode(renderMode: Int): RenderMode? {
                for (mode in entries) {
                    if (mode.value == renderMode) {
                        return mode
                    }
                }
                return null
            }
        }
    }

    enum class FrameRate(val value: Int) {
        FRAME_RATE_FPS_1(1),
        FRAME_RATE_FPS_7(7),
        FRAME_RATE_FPS_10(10),
        FRAME_RATE_FPS_15(15),
        FRAME_RATE_FPS_24(24),
        FRAME_RATE_FPS_30(30),
        FRAME_RATE_FPS_60(60);

        companion object {
            fun getFrameRate(rate: Int): FrameRate? {
                for (frameRate in FrameRate.entries) {
                    if (frameRate.value == rate) {
                        return frameRate
                    }
                }
                return null
            }
        }
    }


    enum class OrientationMode(val value: Int) {
        ADAPTIVE(0),
        FIXED_LANDSCAPE(1),
        FIXED_PORTRAIT(2);

        companion object {
            fun getOrientationMode(mode: Int): OrientationMode? {
                for (orientationMode in OrientationMode.entries) {
                    if (orientationMode.value == mode) {
                        return orientationMode
                    }
                }
                return null
            }
        }
    }

    enum class VideoModulePosition(val value: Int) {
        VIDEO_MODULE_POSITION_POST_CAPTURER(1),
        VIDEO_MODULE_POSITION_PRE_RENDERER(2),
        VIDEO_MODULE_POSITION_PRE_ENCODER(4),
        VIDEO_MODULE_POSITION_POST_CAPTURER_ORIGIN(8);

        companion object {
            fun getVideoModulePosition(position: Int): VideoModulePosition? {
                for (videoModulePosition in VideoModulePosition.entries) {
                    if (videoModulePosition.value == position) {
                        return videoModulePosition
                    }
                }
                return null
            }
        }
    }
}