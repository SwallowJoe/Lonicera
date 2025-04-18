package com.android.mcpsdk.client

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.ParcelFileDescriptor.AutoCloseInputStream
import android.os.ParcelFileDescriptor.AutoCloseOutputStream
import android.util.Log
import com.android.mcpsdk.IPipeService
import com.android.mcpsdk.base.McpConnection
import com.android.mcpsdk.server.PipeService.Companion.ACTION_PIPE_SERVICE
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered

class BinderMcpClient(
    name: String,
    version: String,
    private val context: Context,
): McpConnection(name, version) {

    companion object {
        private const val TAG = "BinderMcpClient"
    }

    private val channel = Channel<Int>(capacity = 1)

    private var iPipeService: IPipeService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            service?.let {
                iPipeService = IPipeService.Stub.asInterface(it)
                channel.trySend(1)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
        }

        override fun onBindingDied(name: ComponentName?) {
        }
    }
    override suspend fun connect(): Boolean {
        val intent = Intent(ACTION_PIPE_SERVICE)
        intent.component = ComponentName("com.android.mcpserverapp",
            "com.android.mcpsdk.server.PipeService")
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        withTimeoutOrNull(3000) {
            channel.receive()
            connectToMcpServer()
        }

        return isConnected()
    }

    private suspend fun connectToMcpServer() {
        iPipeService?.openPipe(name)?.let { fd ->
            val inputStream = AutoCloseInputStream(fd)
            val outputStream = AutoCloseOutputStream(fd)
            val transport = StdioClientTransport(
                inputStream.asSource().buffered(),
                outputStream.asSink().buffered()
            )
            mcpClient.connect(transport)
        }
    }

    override suspend fun close() {
        mcpClient.close()
    }

    override fun isConnected(): Boolean {
        return iPipeService != null
    }
}