package com.llmsdk.tools

import android.content.Context
import android.util.Log
import com.android.mcpsdk.base.McpConnection
import com.android.mcpsdk.client.BinderMcpClient
import com.llmsdk.deepseek.models.FunctionRequest
import com.llmsdk.deepseek.models.Tool
import com.llmsdk.deepseek.models.ToolCallType
import com.llmsdk.tools.functions.getWeatherFunctionRequest
import java.util.concurrent.ConcurrentHashMap

object ToolManager {
    private const val TAG = "ToolManager"

    private data class ToolRecord(
        val tool: Tool,
        val connection: McpConnection?
    )

    private val mTools = ConcurrentHashMap<String, ToolRecord>()
    private var mMcpConnection: McpConnection? = null

    init {
        registerFunction(getWeatherFunctionRequest())
    }

    fun initEnv(context: Context) {
        mMcpConnection = BinderMcpClient("weather", "0.0.1", context)
    }

    suspend fun callTool(function: String, arguments: Map<String, Any?>): String {
        if (mMcpConnection?.isConnected() != true) {
            mMcpConnection?.let {
                it.connect()
                it.fetchAvailableTools().forEach { mcpTool ->
                    registerFunction(mcpTool.toTool(), it)
                }

            }
        }
        val record = mTools[function]
        return record?.connection?.callTool(function, arguments)?.result ?: "[tool call($function) error.]"
    }

    fun availableFunctions(): List<FunctionRequest> {
        return mTools.values.map { it.tool.function }
    }

    fun availableTools(): List<Tool> {
        return mTools.values.map { it.tool }
    }

    fun registerFunction(function: FunctionRequest) {
        if (mTools.containsKey(function.name)) {
            Log.w(TAG, "replace function ${function.name}!")
        }
        mTools[function.name] = ToolRecord(Tool(ToolCallType.FUNCTION, function), null)
    }

    private fun registerFunction(tool: Tool, connection: McpConnection) {
        if (mTools.containsKey(tool.function.name)) {
            Log.w(TAG, "replace function ${tool.function.name}!")
        }
        Log.i(TAG, "registerFunction $tool")
        mTools[tool.function.name] = ToolRecord(tool, connection)
    }

    fun unregisterFunction(function: FunctionRequest) {
        mTools.remove(function.name)
    }
}