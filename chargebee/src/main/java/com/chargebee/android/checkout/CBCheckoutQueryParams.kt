package com.chargebee.android.checkout

import com.chargebee.android.network.Auth

internal class CBCheckoutQueryParams(private val itemPriceId: String,
                                     private val quantity: String) {
    companion object {
        fun fromCheckoutParams(listOfParams: List<String>): CBCheckoutQueryParams {
            return CBCheckoutQueryParams(
                listOfParams[0],
                listOfParams[1]
            )
        }
    }

    fun toCheckoutParams(): Map<String, String> {
        return mapOf(
            "subscription_items[item_price_id][0]" to this.itemPriceId,
            "subscription_items[quantity][0]" to this.quantity
        )
    }

}
