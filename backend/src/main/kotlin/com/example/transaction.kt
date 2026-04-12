package com.example

import io.micronaut.core.annotation.Introspected

@Introspected
data class Transaction(
    val userId: String,
    val amount: Double,
    val merchant: String = "unknown",
    val timestamp: Long = System.currentTimeMillis()
)

@Introspected
data class FraudStatus(
    val userId: String,
    val count: Long,
    val node: String,
    val isBlocked: Boolean = count >= 3
)