package com.android.mcpsdk.client

import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.util.Log
import com.android.mcpsdk.base.IMcpConnectListener
import com.android.mcpsdk.base.McpConnection
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered

class LocalSocketMcpClient(
    name: String,
    version: String,
    private val address: String,
    private val listener: IMcpConnectListener? = null,
): McpConnection(name, version) {

    companion object {
        private const val TAG = "LocalSocketMcpClient"
    }

    private val socket: LocalSocket = LocalSocket()
    override suspend fun connect(): Boolean {
        withContext(Dispatchers.IO) {
            try {
                socket.connect(LocalSocketAddress(address, LocalSocketAddress.Namespace.FILESYSTEM))
                if (socket.isConnected) {
                    val transport = StdioClientTransport(
                        input = socket.inputStream.asSource().buffered(),
                        output = socket.outputStream.asSink().buffered()
                    )
                    mcpClient.connect(transport)
                } else {

                }
            } catch (e: Exception) {
                Log.e(TAG, "connect error", e)
            }
        }

        return isConnected()
    }

    override suspend fun close() {
        try {
            socket.close()
        } catch (_: Exception) {

        }
        listener?.onDisconnected()
    }

    override fun isConnected(): Boolean {
        return socket.isConnected
    }
}