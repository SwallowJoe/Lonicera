package com.llmsdk.deepseek.models

import androidx.annotation.StringDef
import kotlinx.serialization.Serializable

@Serializable
data class ResponseFormat(
    @ResponseFormatType val type: String
) {
    companion object {
        const val TEXT = "text"
        const val JSON = "json_object"

        @Retention(AnnotationRetention.SOURCE)
        @Target(AnnotationTarget.VALUE_PARAMETER)
        @StringDef(TEXT, JSON)
        annotation class ResponseFormatType
    }
}