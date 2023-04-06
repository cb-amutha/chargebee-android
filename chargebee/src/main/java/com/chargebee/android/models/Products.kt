package com.chargebee.android.models

import com.android.billingclient.api.ProductDetails

data class CBProduct(val productId: String,val productTitle:String, val productPrice: String, var productDetails: ProductDetails, var subStatus: Boolean, val offerToken: String? )