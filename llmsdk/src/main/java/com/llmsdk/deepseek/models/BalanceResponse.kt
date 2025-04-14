package com.llmsdk.deepseek.models

import kotlinx.serialization.Serializable

@Serializable
data class BalanceResponse(
    val is_available: Boolean,
    val balance_infos: List<BalanceInfo>
) {
    override fun toString(): String {
        return "BalanceResponse(is_available=$is_available, balance_infos=$balance_infos)"
    }
}