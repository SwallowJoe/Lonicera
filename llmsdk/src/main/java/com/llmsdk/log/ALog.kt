package com.llmsdk.log

import android.util.Log

object ALog {
    private const val PRE_TAG = "Lonicera_"
    private var DEBUG_ALL = true

    @JvmStatic
    fun v(tag: String, message: String, tr: Throwable? = null) {
        if (DEBUG_ALL) {
            Log.v("$PRE_TAG$tag", message, tr)
        }
    }

    @JvmStatic
    fun i(tag: String, message: String, tr: Throwable? = null) {
        if (DEBUG_ALL) {
            Log.i("$PRE_TAG$tag", message, tr)
        }
    }

    @JvmStatic
    fun d(tag: String, message: String, tr: Throwable? = null) {
        if (DEBUG_ALL) {
            Log.d("$PRE_TAG$tag", message, tr)
        }
    }

    @JvmStatic
    fun w(tag: String, message: String, tr: Throwable? = null) {
        Log.w("$PRE_TAG$tag", message, tr)
    }

    @JvmStatic
    fun e(tag: String, message: String, tr: Throwable? = null) {
        Log.e("$PRE_TAG$tag", message, tr)
    }

    @JvmStatic
    fun wtf(tag: String, message: String, tr: Throwable? = null) {
        Log.wtf("$PRE_TAG$tag", message, tr)
    }

}