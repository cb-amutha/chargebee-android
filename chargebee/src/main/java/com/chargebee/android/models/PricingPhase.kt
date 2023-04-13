package com.chargebee.android.models

import com.android.billingclient.api.ProductDetails

data class PricingPhase(
    val billingPeriod: SubscriptionPeriod,
    val recurrenceMode: Int,
    val billingCycleCount: Int?,
    val price: ProductPrice
)

data class ProductPrice(
    val formatted: String,
    val amountMicros: Long,
    val currencyCode: String
)

data class SubscriptionPeriod(
    val periodUnit: String,
    val numberOfUnits: Int
)

enum class RecurrenceMode(val recurrenceMode: Int) {
    INFINITE_RECURRING(1),
    FINITE_RECURRING(2),
    NON_RECURRING(3)
}

fun ProductDetails.PricingPhase.convertCBPricingPhase(): PricingPhase {
    val numberOfUnits = billingPeriod.substring(1, billingPeriod.length - 1).toInt()
    val periodUnit = getPeriodUnit(billingPeriod)
    return PricingPhase(
        SubscriptionPeriod(periodUnit,numberOfUnits),
        recurrenceMode,
        billingCycleCount,
        ProductPrice(formattedPrice, priceAmountMicros, priceCurrencyCode)
    )
}

fun getPeriodUnit(billingPeriod: String ): String {
    return when (billingPeriod.last().toString()) {
        "Y" -> "year"
        "M" -> "month"
        "W" -> "week"
        "D" -> "day"
        else -> ""
    }
}