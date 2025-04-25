package com.llmsdk.deepseek.models

import kotlinx.serialization.Serializable

@Serializable
data class BalanceInfo(
    val currency: String,
    val total_balance: String,
    val granted_balance: String,
    val topped_up_balance: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BalanceInfo) return false

        return currency == other.currency &&
                total_balance == other.total_balance &&
                granted_balance == other.granted_balance &&
                topped_up_balance == other.topped_up_balance
    }

    override fun hashCode(): Int {
        var result = currency.hashCode()
        result = 31 * result + total_balance.hashCode()
        result = 31 * result + granted_balance.hashCode()
        result = 31 * result + topped_up_balance.hashCode()
        result = 31 * result + toString().hashCode()
        return result
    }

    override fun toString(): String {
        return "BalanceInfo(currency='$currency', total_balance='$total_balance', granted_balance='$granted_balance', topped_up_balance='$topped_up_balance')"
    }
}