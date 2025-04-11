package com.llmsdk.deepseek.errors

import com.llmsdk.errors.ResponseError
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess

internal suspend fun validateResponse(response: HttpResponse) {
    if (!response.status.isSuccess()) {
        response.headers
        throw if (response.status.description.isEmpty()) {
            DeepSeekException.from(
                response.status.value,
                response.headers,
                response.body<ResponseError>()
            )
        } else {
            DeepSeekException.from(
                response.status.value, response.headers,
                response.body<ResponseError>(), response.status.description
            )
        }
    }
}