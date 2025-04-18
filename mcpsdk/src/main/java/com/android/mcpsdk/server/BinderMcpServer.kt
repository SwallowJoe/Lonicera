package com.android.mcpsdk.server

import android.os.ParcelFileDescriptor
import android.os.ParcelFileDescriptor.AutoCloseInputStream
import android.os.ParcelFileDescriptor.AutoCloseOutputStream
import android.util.Log
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject


object BinderMcpServer {
    private const val TAG = "BinderMcpServer"
    private const val VERSION = "0.0.1"
    private val mMcpServers = HashSet<Server>()
    private val EmptyJsonObject = JsonObject(emptyMap())

    private data class ToolInfo(
        val name: String,
        val description: String,
        val properties: JsonObject,
        val required: List<String>,
        val handler: suspend (CallToolRequest) -> CallToolResult
     )

    private val mTools = HashSet<ToolInfo>()

    private fun createMcpServer(name: String, version: String): Server = Server(
        serverInfo = Implementation(name, version),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                prompts = ServerCapabilities.Prompts(listChanged = true),
                resources = ServerCapabilities.Resources(subscribe = true, listChanged = true),
                tools = ServerCapabilities.Tools(listChanged = true)
            )
        )
    )

    internal fun newServer(name: String, serverFd: ParcelFileDescriptor) {
        Log.i(TAG, "newServer serverFd: ${serverFd.fd}")

        val server = createMcpServer(name, VERSION)
        configureServer(server)

        val inputStream = AutoCloseInputStream(serverFd)
        val outputStream = AutoCloseOutputStream(serverFd)

        val transport = StdioServerTransport(
            inputStream = inputStream.asSource().buffered(),
            outputStream = outputStream.asSink().buffered()
        )
        transport.onMessage { message ->
            Log.i(TAG, "new mcp server message: $message")
        }

        runBlocking(Dispatchers.IO) {

            server.connect(transport)
            server.onClose {
                synchronized(mMcpServers) {
                    mMcpServers.remove(server)
                }
            }
            synchronized(mMcpServers) {
                mMcpServers.add(server)
            }
        }
    }

    private fun configureServer(server: Server) {
        server.onInitalized {
            Log.i(TAG, "new mcp server init")
        }
        configureResources(server)
        configurePrompts(server)
        configureTools(server)
    }

    private fun configureResources(server: Server) {
    }

    private fun configurePrompts(server: Server) {
    }

    private fun configureTools(server: Server) {
        server.addTool(
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
            val location = request.arguments["location"]?.jsonPrimitive?.content
            Log.i(TAG, "location is $location, so weather is: ${location.hashCode() % 50}°C")
            CallToolResult(
                content = listOf(TextContent("${location.hashCode() % 50}°C"))
            )
        }
        mTools.forEach {
            server.addTool(
                name = it.name,
                description = it.description,
                inputSchema = Tool.Input(
                    properties = it.properties,
                    required = it.required
                ),
                handler = it.handler
            )
        }
    }

    fun addTool(
        name: String,
        description: String,
        properties: JsonObject = EmptyJsonObject,
        required: List<String> = emptyList(),
        handler: suspend (CallToolRequest) -> CallToolResult
    ) {
        mTools.add(ToolInfo(name, description, properties, required, handler))
/*
        val servers = synchronized(mMcpServers) {
            mMcpServers.toList()
        }
        servers.forEach { server ->
            server.addTool(
                name = name,
                description = description,
                inputSchema = Tool.Input(
                    properties = properties,
                    required = required
                ),
                handler = handler
            )
            server.sendToolListChanged()
        }*/
    }
}