package com.zhipu.ai.utils

import com.zhipu.ai.BuildConfig
import io.agora.media.RtcTokenBuilder
import io.agora.rtm.RtmTokenBuilder
import io.agora.rtm.RtmTokenBuilder2
import java.util.Random

object KeyCenter {
    const val USER_MAX_UID = 10000

    val APP_ID: String = BuildConfig.APP_ID
    private var USER_RTC_UID = -1

    private val randomUserUid: Int
        get() {
            USER_RTC_UID = Random().nextInt(USER_MAX_UID)
            return USER_RTC_UID
        }

    fun getUid(): Int {
        if (USER_RTC_UID == -1) {
            USER_RTC_UID = randomUserUid
        }
        return USER_RTC_UID
    }

    fun getRtcToken(channelId: String?, uid: Int): String {
        return RtcTokenBuilder().buildTokenWithUid(
            APP_ID,
            BuildConfig.APP_CERTIFICATE,
            channelId,
            uid,
            RtcTokenBuilder.Role.Role_Publisher,
            0
        )
    }

    fun getRtmToken(uid: Int): String? {
        return try {
            RtmTokenBuilder().buildToken(
                APP_ID,
                BuildConfig.APP_CERTIFICATE, uid.toString(),
                RtmTokenBuilder.Role.Rtm_User,
                0
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getRtmToken2(uid: Int): String? {
        return try {
            RtmTokenBuilder2().buildToken(
                APP_ID,
                BuildConfig.APP_CERTIFICATE, uid.toString(),
                24 * 60 * 60
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
