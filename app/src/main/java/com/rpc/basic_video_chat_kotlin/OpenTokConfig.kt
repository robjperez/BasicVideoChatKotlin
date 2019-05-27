package com.rpc.basic_video_chat_kotlin

import android.webkit.URLUtil

object OpenTokConfig {
    // *** Fill the following variables using your own Project info from the OpenTok dashboard  ***
    // ***                      https://dashboard.tokbox.com/projects                           ***

    // Replace with your OpenTok API key
    const val API_KEY = ""
    // Replace with a generated Session ID
    const val SESSION_ID = ""
    // Replace with a generated token (from the dashboard or using an OpenTok server SDK)
    const val TOKEN = ""

    internal var hardCodedConfigErrorMessage: String = ""


    fun areHardCodedConfigsValid(): Boolean {
        return if (API_KEY != null && !API_KEY.isEmpty()
            && SESSION_ID != null && !SESSION_ID.isEmpty()
            && TOKEN != null && !TOKEN.isEmpty()
        ) {
            true
        } else {
            hardCodedConfigErrorMessage = "API KEY, SESSION ID and TOKEN in OpenTokConfig.java cannot be null or empty."
            false
        }
    }
}
