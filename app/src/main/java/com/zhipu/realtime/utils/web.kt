package com.zhipu.realtime.utils

import com.google.gson.annotations.SerializedName

data class RootResponse<T>(
    @SerializedName("code") val code: Int,
    @SerializedName("msg") val msg: String,
    @SerializedName("data") val data: T,
    @SerializedName("success") val success: Boolean
)

data class RoomResponseData(
    @SerializedName("auth_token") val authToken: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("user_id") val userId: String,
    @SerializedName("room_id") val roomId: String,
    @SerializedName("server_user_id") val serverUserId: String
)