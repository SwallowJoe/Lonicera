package com.llmsdk.deepseek.models

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ChatCompletionMessageSerializerTest {

    private lateinit var json: Json

    @Before
    fun setUp() {
        json = Json {
            serializersModule = ChatMessageModule
        }
    }

    @Test
    fun systemMessage_WithContentAndName_ShouldCreateCorrectInstance() {
        val message = SystemMessage(content = "Hello", name = "System")
        assertEquals("Hello", message.content)
        assertEquals("System", message.name)
    }

    @Test
    fun systemMessage_WithOnlyContent_ShouldCreateCorrectInstance() {
        val message = SystemMessage(content = "Hello")
        assertEquals("Hello", message.content)
        assertEquals(null, message.name)
    }
}