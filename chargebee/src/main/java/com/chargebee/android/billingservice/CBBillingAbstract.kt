package com.chargebee.android.billingservice

import com.android.billingclient.api.BillingClient
import com.chargebee.android.models.CBProduct

abstract class CBBillingAbstract {
   // abstract fun loadProductDetails(@BillingClient.SkuType skuType: String)
    abstract fun isConnected() : Boolean
    abstract fun startConnection()
    protected abstract fun endConnection()
   // abstract fun purchase(product: CBProduct, purchaseCallBack: CBCallback.PurchaseCallback<String>)
}