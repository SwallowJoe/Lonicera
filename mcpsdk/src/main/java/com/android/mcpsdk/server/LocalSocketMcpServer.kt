package com.android.mcpsdk.server

import android.net.LocalServerSocket
import android.system.Os
import android.system.OsConstants
import android.util.Log
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class LocalSocketMcpServer(
    private val name: String,
    private val version: String,
    private val address: String,
) {
    companion object {
        private const val TAG = "LocalSocketMcpServer"
    }
    private var socketServer: LocalServerSocket? = null
    @Volatile private var mRunning = true

    private val mMcpServers = HashSet<Server>()
    private fun newMcpServer(): Server = Server(
        serverInfo = Implementation(name, version),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                prompts = ServerCapabilities.Prompts(listChanged = true),
                resources = ServerCapabilities.Resources(subscribe = true, listChanged = true),
                tools = ServerCapabilities.Tools(listChanged = true)
            )
        )
    )

    fun start() {
        Thread {
            try {
                socketServer = LocalServerSocket(address)
                while (mRunning) {
                    socketServer?.accept()?.let { socket ->
                        val transport = StdioServerTransport(
                            inputStream = socket.inputStream.asSource().buffered(),
                            outputStream = socket.outputStream.asSink().buffered()
                        )
                        CoroutineScope(Dispatchers.IO).launch {
                            val mcpServer = newMcpServer()
                            mcpServer.addTool(
                                name = "get_weather",
                                description = "Get weather of an location, the user should supply a location first",
                                inputSchema = Tool.Input(
                                    properties = buildJsonObject {
                                        putJsonObject("location") {
                                            put("type", "string")
                                            put("description", "The city and state, e.g. San Francisco, CA")
                                        }
                                    },
                                    required = listOf("location")
                                )
                            ) { request ->
                                CallToolResult(
                                    content = listOf(TextContent("name=${request.name}, method=${request.method}"))
                                )
                            }
                            mcpServer.connect(transport)
                            mcpServer.onClose {
                                synchronized(mMcpServers) {
                                    mMcpServers.remove(mcpServer)
                                }
                                socket.close()
                            }
                            synchronized(mcpServer) {
                                mMcpServers.add(mcpServer)
                            }
                        }

                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "mcp server start failed", e)
            } finally {
                try {
                    socketServer?.let {
                        Os.shutdown(it.fileDescriptor, OsConstants.SHUT_RDWR);
                        it.close()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "mcp server close failed", e)
                }
            }
        }.start()
    }

    fun close() {
        mRunning = false
    }

    fun addTool(name: String, description: String, inputSchema: Tool.Input) {
        val servers = synchronized(mMcpServers) {
            mMcpServers.toList()
        }
        servers.forEach { mcpServer ->
            mcpServer.addTool(
                name = name,
                description = description,
                inputSchema = inputSchema
            ) { request ->

                CallToolResult(
                    content = listOf(TextContent("name=${request.name}, method=${request.method}"))
                )
            }
        }
    }
}