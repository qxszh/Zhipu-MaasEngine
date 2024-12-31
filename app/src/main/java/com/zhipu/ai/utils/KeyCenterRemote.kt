package com.zhipu.ai.utils

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zhipu.ai.BuildConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

object KeyCenterRemote {
    private const val SERVER_URL: String = BuildConfig.REMOTE_TOKEN_URL
    private const val REMOTE_AUTH_TOKEN: String = BuildConfig.REMOTE_AUTH_TOKEN
    private const val LOG_TAG = "KeyCenter"

    private val client = OkHttpClient()
    private val gson = Gson()

    sealed class Result {
        data class Success(val token: String, val userId: Int, val channelId: String) : Result()
        data class Failure(val errorMessage: String) : Result()
    }

    // 对外接口
    fun getRtcData(model: String, callback: (Result) -> Unit) {
        requestRtcToken(model, callback)
    }

    private fun requestRtcToken(model: String, callback: (Result) -> Unit) {
        // 参数说明：
        //     voiceType - 邻家少女 青春少年 两种音色
        //     rtcType - agora 声网RTC服务
        //     systemPrompt - 系统提示词
        //     welcomeText - 自定义欢迎语
        val json = """
            {
                "rtcType": "agora",
                "model": "$model",
                "voiceType": "青春少年", 
                "systemPrompt": "你是一个名叫\"小智\"的人工智能助手，针对用户的问题和要求提供适当的答复和支持",
                "welcomeText": "你好，我是小智手机助手，很高兴为您服务"
            }
        """.trimIndent()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = json.toRequestBody(mediaType)

        val request = Request.Builder().url(SERVER_URL).addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer " + REMOTE_AUTH_TOKEN).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(LOG_TAG, "Failed to request RTC token", e)
                callback(Result.Failure("Failed to request RTC token: ${e.message}"))
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful && response.body != null) {
                    val responseBody = response.body!!.string()
                    try {
                        val type = object : TypeToken<RootResponse<RoomResponseData>>() {}.type
                        val rootResponse: RootResponse<RoomResponseData> = gson.fromJson(responseBody, type)
                        val roomData = rootResponse.data

                        val rtcToken = roomData.authToken
                        val rtcUserId = try {
                            roomData.userId.toInt()
                        } catch (e: NumberFormatException) {
                            Log.e(LOG_TAG, "Failed to convert userId to Int: ${e.message}")
                            -1
                        }
                        val rtcChannelId = roomData.roomId

                        callback(Result.Success(rtcToken, rtcUserId, rtcChannelId))
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "Failed to parse JSON response: ${e.message}")
                        callback(Result.Failure("Failed to parse JSON response: ${e.message}"))
                    }
                } else {
                    Log.e(LOG_TAG, "Failed to retrieve RTC token, response: ${response.message}")
                    callback(Result.Failure("Failed to retrieve RTC token, response code: ${response.code}"))
                }
            }
        })
    }
}