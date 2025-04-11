package com.llmsdk.base

import java.net.http.HttpClient

abstract class Client(
    private val httpClient: HttpClient,
    private val config: String
) {
}