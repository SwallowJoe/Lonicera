package com.android.mcpserverapp.tools

import android.util.Log
import com.android.mcpsdk.server.BinderMcpServer
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

suspend fun registerCalendar() {
    BinderMcpServer.addTool(
        name = "add_calendar",
        description = "Add a calendar event on the device, user must supply title, description, and reminder time first.",
        properties = buildJsonObject {
            putJsonObject("title") {
                put("type", "string")
                put("description", "The title of the calendar event")
            }
            putJsonObject("description") {
                put("type", "string")
                put("description", "The description of the calendar event")
            }
            putJsonObject("reminderTime") {
                put("type", "number")
                put("description", "Calendar reminder time, in milliseconds since the Unix epoch. For example, 1640995200000 represents 2022-01-01 00:00:00. Alternatively, if the user says 'in two days', it's the timestamp of 9 AM two days after the current time.")
            }
        },
        required = listOf("title", "description", "reminderTime")
    ) { request ->
        val title = request.arguments["title"]?.jsonPrimitive?.content
        val description = request.arguments["description"]?.jsonPrimitive?.content
        val reminderTime = request.arguments["reminderTime"]?.jsonPrimitive?.content?.toLong()
        Log.i("registerCalendar", "registerCalendar $title, $description, $reminderTime")
        CallToolResult(
            content = listOf(
                TextContent(
                    text = "日历添加成功！"
                )
            )
        )
    }
}