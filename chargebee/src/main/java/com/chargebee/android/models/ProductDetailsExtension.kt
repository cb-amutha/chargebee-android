package com.chargebee.android.models

import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails

fun List<ProductDetails>.toCBProducts(): List<CBProduct> {
    val cbProducts = mutableListOf<CBProduct>()
    forEach { productDetails ->
        val basePlansList =
            productDetails.subscriptionOfferDetails?.filter { it.isBasePlan } ?: emptyList()
        val offerDetailsByPlanId = productDetails.subscriptionOfferDetails?.groupBy {
            it.basePlanId
        } ?: emptyMap()
        basePlansList.takeUnless { it.isEmpty() }?.forEach { basePlan ->
            val offerDetailsForBasePlan =
                offerDetailsByPlanId[basePlan.basePlanId] ?: emptyList()
            productDetails.toCBProduct(offerDetailsForBasePlan).let {
                cbProducts.add(it)
            }
        } ?: productDetails.toInAppStoreProduct().let {
            cbProducts.add(it)
        }
    }
    return cbProducts
}

fun ProductDetails.toCBProduct(
    subscriptionOfferDetails: List<ProductDetails.SubscriptionOfferDetails>
): CBProduct {

    val subscriptionOffers = subscriptionOfferDetails.map {
        it.convertToCBSubscriptionOffer(productId, this)
    }
    val basePlanPrice = subscriptionOffers.map {
        it.productPrice()
    }
    val basePlanPeriod = subscriptionOffers.map {
        it.subscriptionPeriod()
    }
    val productPrice = createOneTimeProductPrice() ?: basePlanPrice.lastOrNull()
    val subscriptionPeriod = basePlanPeriod.lastOrNull()
    Log.i("TAG", "productPrice : $productPrice")
    Log.i("TAG", "subscriptionPeriod : $subscriptionPeriod")
    return CBProduct(
        productId = productId,
        productType = productType,
        productTitle = title,
        productName = name,
        productDescription = description,
        productPrice = productPrice,
        subscriptionPeriod = subscriptionPeriod,
        null,
        subscriptionOffers,
        subStatus = false)
}

private fun ProductDetails.createOneTimeProductPrice(): ProductPrice? {
    return if (productType == BillingClient.ProductType.INAPP) {
        oneTimePurchaseOfferDetails?.let {
            ProductPrice(
                it.formattedPrice,
                it.priceAmountMicros,
                it.priceCurrencyCode
            )
        }
    } else null
}

fun ProductDetails.toInAppStoreProduct(): CBProduct = this.toCBProduct(emptyList())

val ProductDetails.SubscriptionOfferDetails.isBasePlan: Boolean
    get() = this.pricingPhases.pricingPhaseList.size == 1

