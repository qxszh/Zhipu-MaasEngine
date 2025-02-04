package com.zhipu.realtime.utils

import android.content.Context
import android.util.Log
import com.zhipu.realtime.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

object KeyCenterRemote {
    private const val LOG_TAG = "KeyCenter"

    private val client = OkHttpClient()
    private val gson = Gson()

    sealed class Result {
        data class Success(val token: String, val userId: Int, val channelId: String) : Result()
        data class Failure(val errorMessage: String) : Result()
    }

    fun getRtcData(apikey: String, envToUse: String, customSystemPrompt: String, callback: (Result) -> Unit) {
        var systemPrompt = """你是一个人工智能手机助手""".trimIndent().replace("\n", "\\n")
        if (customSystemPrompt.isNotEmpty()) {
            systemPrompt = customSystemPrompt.trimIndent()
        }
        val jsonObject = JsonObject().apply {
            addProperty("rtcType", "agora")
            addProperty("model", "")
            addProperty("voiceType", "青春少年")
            addProperty("systemPrompt", systemPrompt)
            addProperty("welcomeText", "你好啊")
        }
        val gson = Gson()
        val json = gson.toJson(jsonObject)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = json.toRequestBody(mediaType)


        var serverUrl: String = BuildConfig.REMOTE_TOKEN_URL
        if (envToUse.contentEquals("预发布环境")) {
            serverUrl = BuildConfig.REMOTE_TOKEN_URL_DEV
        }

        val request = Request.Builder().url(serverUrl).addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apikey").post(requestBody).build()

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

                        if (rootResponse.code != 200) {
                            callback(Result.Failure("获取RTC房间信息失败, 错误码:${rootResponse.code}, 错误信息:${rootResponse.msg}"))
                            return
                        }

                        val roomData = rootResponse.data

                        val rtcToken = roomData.authToken
                        val rtcUserId = try {
                            roomData.userId.toInt()
                        } catch (e: NumberFormatException) {
                            Log.e(LOG_TAG, "获取RTC房间信息失败, 无法将userId转为Int类型: ${e.message}")
                            -1
                        }
                        val rtcChannelId = roomData.roomId

                        callback(Result.Success(rtcToken, rtcUserId, rtcChannelId))
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "获取RTC房间信息失败: ${e.message}")
                        callback(Result.Failure("获取RTC房间信息失败: ${e.message}"))
                    }
                } else {
                    Log.e(LOG_TAG, "获取RTC房间信息失败, 错误码:${response.code}, 错误信息:${response.message}")
                    callback(Result.Failure("获取RTC房间信息失败, 错误码:${response.code}, 错误信息:${response.message}"))
                }
            }
        })
    }
}