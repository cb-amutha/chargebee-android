package com.chargebee.android.models

import com.android.billingclient.api.ProductDetails

data class CBSubscriptionOffer(
    val purchaseParams:CBPurchaseParams
)

data class CBPurchaseParams(
    val productId:String,
    val basePlanId:String,
    val offerId: String?,
    val pricingPhase: List<PricingPhase>,
    val productDetails: ProductDetails?,
    val offerToken: String,
)

fun ProductDetails.SubscriptionOfferDetails.convertToCBSubscriptionOffer(
    productId: String,
    productDetails: ProductDetails
): CBSubscriptionOffer {
    val pricingPhases = pricingPhases.pricingPhaseList.map { it.convertCBPricingPhase() }
   // Log.i("CBSubscriptionOffer","pricingPhases : $pricingPhases")

    return CBSubscriptionOffer(
        CBPurchaseParams(productId,
        basePlanId,
        offerId,
        pricingPhases,
        productDetails,
        offerToken)
    )
}

fun CBSubscriptionOffer.subscriptionPeriod(): SubscriptionPeriod? {
    return this.purchaseParams.pricingPhase.lastOrNull()?.billingPeriod
}
fun CBSubscriptionOffer.productPrice(): ProductPrice? {
    return if (isBasePlan()){
        this.purchaseParams.pricingPhase.lastOrNull()?.price
    } else null
}

fun CBSubscriptionOffer.isBasePlan(): Boolean {
    return this.purchaseParams.pricingPhase.size == 1
}

fun CBSubscriptionOffer.freeTrialPhase(): PricingPhase? {
     return this.purchaseParams.pricingPhase.dropLast(1).firstOrNull {
        it.price.amountMicros == 0L
    }
}

fun CBSubscriptionOffer.introOfferPhase(): PricingPhase? {
    return this.purchaseParams.pricingPhase.dropLast(1).firstOrNull {
        it.price.amountMicros > 0L
    }
}
