package com.android.lonicera.components.settings

import com.android.lonicera.R
import com.android.lonicera.components.settings.ui.SettingsOption

object SettingsRepository {
    val displaySettingsOptions = listOf(
        SettingsOption(
            startIcon = R.drawable.captive_portal_48px,
            title = "数据管理",
            endIcon = R.drawable.page_info_48px
        ),
        SettingsOption(
            startIcon = R.drawable.captive_portal_48px,
            title = "语言",
            description = "跟随系统"
        ),
        SettingsOption(
            startIcon = R.drawable.captive_portal_48px,
            title = "外观",
            description = "跟随系统"
        )
    )
}