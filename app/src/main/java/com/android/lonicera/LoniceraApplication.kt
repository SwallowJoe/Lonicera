package com.android.lonicera

import android.app.Application
import com.llmsdk.tools.ToolManager

class LoniceraApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        ToolManager.initEnv(this)
    }
}