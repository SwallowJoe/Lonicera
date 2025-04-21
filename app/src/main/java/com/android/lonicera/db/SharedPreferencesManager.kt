package com.android.lonicera.db

import android.content.Context

object SharedPreferencesManager {
    fun save(context: Context, table: String, data: HashMap<String, String>) {
        // 获取 SharedPreferences 对象
        val sharedPref = context.getSharedPreferences(table, Context.MODE_PRIVATE)

        // 创建编辑器
        val editor = sharedPref.edit()

        // 添加数据
        data.forEach { (key, value) ->
            editor.putString(key, value)
        }

        // 提交更改
        editor.apply() // 或者使用 commit()，apply() 是异步的，commit() 是同步的
    }

    fun read(context: Context, table: String, title: String, defaultValue: String): String {
        // 获取 SharedPreferences 对象
        val sharedPref = context.getSharedPreferences(table, Context.MODE_PRIVATE)
        return sharedPref.getString(title, defaultValue) ?: defaultValue
    }
}