package com.chargebee.android.models

import com.android.billingclient.api.ProductDetails

data class CBProduct(
    val productId: String,
    val productType: String,
    val productTitle:String,
    val productName:String,
    val productDescription:String,
    val productPrice: ProductPrice?,
    val subscriptionPeriod: SubscriptionPeriod?,
    val productDetails: ProductDetails?,
    val subscriptionOffers: List<CBSubscriptionOffer>,
    var subStatus: Boolean)
