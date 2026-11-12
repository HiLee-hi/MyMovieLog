package com.mymovie.log.util

import android.util.Log
import com.mymovie.log.BuildConfig

/**
 * App-wide logger.
 * - d(): printed only in DEBUG builds (detailed flow tracing)
 * - i(): always printed (important business events — login, save, delete, etc.)
 * - w(): always printed (abnormal state that is not a crash)
 * - e(): always printed (errors and exceptions)
 */
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

    /** Mask email: abc@gmail.com → a***@gmail.com */
    fun maskEmail(email: String): String {
        val atIndex = email.indexOf('@')
        return if (atIndex > 1) "${email[0]}***${email.substring(atIndex)}" else "***"
    }

    /** Show only the first 8 characters of an ID (e.g. long UUIDs) */
    fun shortId(id: String): String = if (id.length > 8) "${id.take(8)}…" else id
}
