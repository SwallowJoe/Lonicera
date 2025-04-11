package com.android.mcpclient

class McpClient(
    private val name: String,
    private val version: String,
) : AutoCloseable {
    
    private val deepseekApiKey = "sk-cb04244cd4e44584a57e62c6ba4d7d55"


    override fun close() {
    }
}