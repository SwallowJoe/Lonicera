package com.llmsdk.client

import com.llmsdk.deepseek.DeepSeekConfig
import io.ktor.client.HttpClient


internal abstract class BaseClient(
    val client: HttpClient, val config: DeepSeekConfig
): AutoCloseable {

    abstract class Builder(token: String) {

    }
}