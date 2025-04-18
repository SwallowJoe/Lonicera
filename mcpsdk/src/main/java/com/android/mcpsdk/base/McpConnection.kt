package com.android.mcpsdk.base

import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.client.Client

abstract class McpConnection(
    val name: String,
    val version: String
) {

    protected val mcpClient = Client(Implementation(name, version))

    open class Result(
        val result: String? = null,
        val isError: Boolean = false,
    ) {
        object ERROR : Result(isError = true)
    }

    abstract suspend fun connect(): Boolean
    abstract suspend fun close()

    abstract fun isConnected(): Boolean

    suspend fun fetchAvailableTools(): List<Tool> {
        if (!isConnected()) return emptyList()
        return mcpClient
            .listTools()
            ?.tools
            .orEmpty()
    }
    suspend fun callTool(functionName: String, arguments: Map<String, Any?>): Result {
        if (!isConnected()) return Result.ERROR
        val result = mcpClient.callTool(
            name = functionName,
            arguments = arguments,
        )
        if (result == null || result.isError == true) return Result.ERROR
        result.content[0]
        return Result(result = result.content.joinToString())
    }
}