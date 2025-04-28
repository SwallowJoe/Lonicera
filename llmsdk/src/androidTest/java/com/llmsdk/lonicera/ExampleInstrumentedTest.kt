package com.llmsdk.lonicera

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.llmsdk.deepseek.models.ChatChoiceChunk
import com.llmsdk.deepseek.models.ChatCompletionResponseChunk
import com.llmsdk.deepseek.models.ChatMessageModule
import kotlinx.serialization.json.Json

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        val jsonConfig: Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            allowStructuredMapKeys = true
            allowSpecialFloatingPointValues = true
            serializersModule = ChatMessageModule
        }

/*        val content =
            """{"id":"005f7c6d-58f1-4b65-9002-3948871ad5dd","object":"chat.completion.chunk","created":1745812004,"model":"deepseek-chat","system_fingerprint":"fp_8802369eaa_prod0425fp8","choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"id":"call_0_7abe89e6-31e5-4fba-a209-8ae4d6f90d76","type":"function","function":{"name":"get_weather","arguments":""}}]},"logprobs":null,"finish_reason":null}]}""".trimIndent()
        val chunk = jsonConfig.decodeFromString<ChatCompletionResponseChunk>(content)
        assert(chunk.choices?.size == 1)*/


        val content2 =
            """{"id":"462553fa-4cd4-4350-8db0-30d85b6a576e","object":"chat.completion.chunk","created":1745822716,"model":"deepseek-chat","system_fingerprint":"fp_8802369eaa_prod0425fp8","choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"function":{"arguments":"{\""}}]},"logprobs":null,"finish_reason":null}]}"""
        val chunk2 = jsonConfig.decodeFromString<ChatCompletionResponseChunk>(content2)
        assert(chunk2.choices?.size == 1)
    }
}