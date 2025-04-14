package com.llmsdk.deepseek.models

import kotlinx.serialization.Serializable

@Serializable
data class BalanceInfo(
    val currency: String,
    val total_balance: String,
    val granted_balance: String,
    val topped_up_balance: String
) {
    override fun toString(): String {
        return "BalanceInfo(currency='$currency', total_balance='$total_balance', granted_balance='$granted_balance', topped_up_balance='$topped_up_balance')"
    }
}