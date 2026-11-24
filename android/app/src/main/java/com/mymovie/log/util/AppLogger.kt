package com.mymovie.log.util

import android.util.Log
import com.mymovie.log.BuildConfig

// d(): debug builds only, i() / w() / e(): always (events, abnormal state, errors)
object AppLogger {

    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
    }

    fun w(tag: String, message: String) {
        Log.w(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
    }

    // abc@gmail.com -> a***@gmail.com
    fun maskEmail(email: String): String {
        val atIndex = email.indexOf('@')
        return if (atIndex > 1) "${email[0]}***${email.substring(atIndex)}" else "***"
    }

    // first 8 chars of an id (long UUIDs)
    fun shortId(id: String): String = if (id.length > 8) "${id.take(8)}…" else id
}
