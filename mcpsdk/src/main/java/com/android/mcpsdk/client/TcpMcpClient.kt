package com.android.mcpsdk.client

import android.util.Log
import com.android.mcpsdk.base.IMcpConnectListener
import com.android.mcpsdk.base.McpConnection
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import java.net.InetAddress
import java.net.Socket

class TcpMcpClient(
    name: String,
    version: String,
    private val ip: String,
    private val port: Int,
    private val listener: IMcpConnectListener? = null,
) : McpConnection(name, version) {
    companion object {
        private const val TAG = "TcpMcpClient"
    }
    private val mIOScope = CoroutineScope(Dispatchers.IO)
    private var mSocket: Socket? = null
    override suspend fun connect(): Boolean {
        withContext(mIOScope.coroutineContext) {
            var socket: Socket? = null
            try {
                socket = Socket(InetAddress.getByName(ip), port)
                if (socket.isConnected) {
                    val transport = StdioClientTransport(
                        input = socket.getInputStream().asSource().buffered(),
                        output = socket.getOutputStream().asSink().buffered()
                    )
                    mcpClient.connect(transport)
                    mSocket = socket // 只有连接成功后才赋值
                } else {}
            } catch (e: Exception) {
                // 记录异常日志
                Log.e(TAG, "Failed to establish transport $name", e)
            } finally {
                // 如果连接失败，确保关闭 Socket
                if (mSocket != socket) {
                    socket?.close()
                }
            }
        }

        return isConnected()
    }

    override fun isConnected(): Boolean {
        return mSocket?.isConnected ?: false
    }

    override suspend fun close() {
        withContext(mIOScope.coroutineContext) {
            mSocket?.close()
            listener?.onDisconnected()
        }
    }
}