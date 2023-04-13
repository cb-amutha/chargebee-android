package com.chargebee.android.billingservice

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode.*
import com.chargebee.android.ErrorDetail
import com.chargebee.android.exceptions.CBException
import com.chargebee.android.exceptions.ChargebeeResult
import com.chargebee.android.models.CBProduct
import com.chargebee.android.models.CBPurchaseParams
import com.chargebee.android.models.toCBProducts
import com.chargebee.android.network.CBReceiptResponse
import java.util.*

class BillingClientManager constructor(
    context: Context,
    skuList: ArrayList<String>, callBack: CBCallback.ListProductsCallback<ArrayList<CBProduct>>
) : BillingClientStateListener, PurchasesUpdatedListener {

    private val CONNECT_TIMER_START_MILLISECONDS = 1L * 1000L
    lateinit var billingClient: BillingClient
    private var mContext : Context? = null
    private val handler = Handler(Looper.getMainLooper())
    private var skuList = arrayListOf<String>()
    private var callBack : CBCallback.ListProductsCallback<ArrayList<CBProduct>>
    private var purchaseCallBack: CBCallback.PurchaseCallback<String>? = null
    private val skusWithSkuDetails = arrayListOf<CBProduct>()
    private val TAG = javaClass.simpleName
    lateinit var product: CBProduct
    private var offerToken: String? = null
    private var customerID: String? = null

    init {
        mContext = context
        this.skuList = skuList
        this.callBack = callBack
        startBillingServiceConnection()
    }

    /* Called to notify that the connection to the billing service was lost*/
    override fun onBillingServiceDisconnected() {
        connectToBillingService()
    }

    /* The listener method will be called when the billing client setup process complete */
    override fun onBillingSetupFinished(billingResult: BillingResult) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                Log.i(
                    TAG,
                    "Google Billing Setup Done!"
                )
                loadProductDetails(setOf(CBPurchase.ProductType.SUBS, CBPurchase.ProductType.INAPP))
            }
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED,
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                callBack.onError(CBException(ErrorDetail(message = GPErrorCode.BillingUnavailable.errorMsg, httpStatusCode = billingResult.responseCode)))
                Log.i(TAG, "onBillingSetupFinished() -> with error: ${billingResult.debugMessage}")
            }
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            BillingClient.BillingResponseCode.USER_CANCELED,
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
            BillingClient.BillingResponseCode.ERROR,
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED,
            BillingClient.BillingResponseCode.SERVICE_TIMEOUT,
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> {
                Log.i(
                    TAG,
                    "onBillingSetupFinished() -> google billing client error: ${billingResult.debugMessage}"
                )
            }
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> {
                Log.i(
                    TAG,
                    "onBillingSetupFinished() -> Client is already in the process of connecting to billing service"
                )

            }
            else -> {
                Log.i(TAG, "onBillingSetupFinished -> with error: ${billingResult.debugMessage}.")
            }
        }
    }

    /* Method used to configure and create a instance of billing client */
    private fun startBillingServiceConnection() {
        billingClient = mContext?.let {
            BillingClient.newBuilder(it)
                .enablePendingPurchases()
                .setListener(this).build()
        }!!

        connectToBillingService()
    }
    /* Connect the billing client service */
    private fun connectToBillingService() {
        if (!billingClient.isReady) {
            handler.postDelayed(
                { billingClient.startConnection(this@BillingClientManager) },
                CONNECT_TIMER_START_MILLISECONDS
            )
        }
    }

    private fun buildQueryProductDetailsParams(productType: String): QueryProductDetailsParams {
        val productList = mutableListOf<QueryProductDetailsParams.Product>()
        for (product in skuList) {
            productList.add(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(product)
                    .setProductType(productType)
                    .build()
            )
        }
        return QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
    }

    private fun getProductDetails(types: Set<CBPurchase.ProductType>, callBack: CBCallback.ProductsCallback<List<CBProduct>>) {
        try {
            val productTypes = types.toMutableSet()
            val type = productTypes.firstOrNull()?.also { productTypes.remove(it) }
            type?.let {
                billingClient.queryProductDetailsAsync(buildQueryProductDetailsParams(it.value)) { billingResult, productDetailsList ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        try {
                            val storeProducts = productDetailsList.toCBProducts()
                            skusWithSkuDetails.addAll(storeProducts)
                            loadProductDetails(productTypes)
                            if (productTypes.size ==0){
                                callBack.onSuccess(productDetails = skusWithSkuDetails)
                            }
                        } catch (ex: CBException) {
                            callBack.onError(CBException(ErrorDetail(GPErrorCode.UnknownError.errorMsg)))
                        }
                    } else {
                        callBack.onError(CBException(ErrorDetail("Service Unavailable")))
                    }

                }
            }
        }catch (exp: Exception){
            callBack.onError(CBException(ErrorDetail("${exp.message}")))
        }
    }

    /* Get the Product Details from Play Console */
    private fun loadProductDetails(types: Set<CBPurchase.ProductType>) {
        getProductDetails(types, object : CBCallback.ProductsCallback<List<CBProduct>> {
            override fun onSuccess(productDetails: List<CBProduct>) {
                callBack.onSuccess(skusWithSkuDetails)
            }
            override fun onError(error: CBException) {
                callBack.onError(error)
            }

        })
    }
    private fun buildBillingFlowParams(purchaseParams: CBPurchaseParams, billingParams: (BillingFlowParams) -> Unit)  {
        val productDetailsParamsList = listOf(
            purchaseParams.productDetails?.let { it ->
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(it)
                    .setOfferToken(purchaseParams.offerToken)
                    .build()
            }
        )
        billingParams(BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList).build())
    }

    /* Purchase the product: Initiates the billing flow for an In-app-purchase  */
    fun purchaseProduct(
        purchaseParams: CBPurchaseParams?,
        customerID: String = "",
        purchaseCallBack: CBCallback.PurchaseCallback<String>
    ) {
        this.purchaseCallBack = purchaseCallBack
        if (!(TextUtils.isEmpty(customerID))) {
            this.customerID = customerID
        }

        Log.i(TAG, "offerToken : ${purchaseParams?.offerToken} ")

        if (purchaseParams != null) {
            buildBillingFlowParams(purchaseParams){
                billingClient.launchBillingFlow(mContext as Activity, it)
                    .takeIf { billingResult -> billingResult.responseCode != BillingClient.BillingResponseCode.OK
                    }?.let { billingResult ->
                        Log.e(TAG, "Failed to launch billing flow $billingResult")
                    }
            }
        }
    }


    /* Purchase the product: Initiates the billing flow for an In-app-purchase  */
    fun purchase(
        product: CBProduct,
        customerID: String = "",
        purchaseCallBack: CBCallback.PurchaseCallback<String>
    ) {
        this.purchaseCallBack = purchaseCallBack
        if (!(TextUtils.isEmpty(customerID))) {
            this.customerID = customerID
        }
        Log.i(TAG, "offerToken : ${product.subscriptionOffers.firstOrNull()?.purchaseParams?.offerToken} ")

        val purchaseParams = product.subscriptionOffers.firstOrNull()?.purchaseParams
        if (purchaseParams != null) {
            buildBillingFlowParams(purchaseParams){
                billingClient.launchBillingFlow(mContext as Activity, it)
                    .takeIf { billingResult -> billingResult.responseCode != BillingClient.BillingResponseCode.OK
                    }?.let { billingResult ->
                        Log.e(TAG, "Failed to launch billing flow $billingResult")
                    }
            }
        }

//        val productDetailsParamsList = listOf(
//            purchaseParams?.productDetails?.let { it ->
//                BillingFlowParams.ProductDetailsParams.newBuilder()
//                    .setProductDetails(it)
//                    .setOfferToken(purchaseParams.offerToken)
//                    .build()
//            }
//        )
//
//        val billingFlowParams = BillingFlowParams.newBuilder()
//            .setProductDetailsParamsList(productDetailsParamsList).build()
//
//        billingClient.launchBillingFlow(mContext as Activity, billingFlowParams)
//            .takeIf { billingResult -> billingResult.responseCode != BillingClient.BillingResponseCode.OK
//            }?.let { billingResult ->
//                Log.e(TAG, "Failed to launch billing flow $billingResult")
//            }

    }

    /* Checks if the specified feature is supported by the Play Store */
    fun isFeatureSupported(): Boolean {
        try {
            val featureSupportedResult = billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
            when(featureSupportedResult.responseCode){
                BillingClient.BillingResponseCode.OK -> {
                    return true
                }
                BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> {
                    return false
                }
            }
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Play Services not available ")
        }
        return false
    }

    /* Checks if the billing client connected to the service */
    fun isBillingClientReady(): Boolean{
        return billingClient.isReady
    }

    /* Google Play calls this method to deliver the result of the Purchase Process/Operation */
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            OK -> {
                purchases?.forEach { purchase ->
                    when (purchase.purchaseState) {
                        Purchase.PurchaseState.PURCHASED -> {
                            acknowledgePurchase(purchase)
                        }
                        Purchase.PurchaseState.PENDING -> {
                            purchaseCallBack?.onError(CBException(ErrorDetail(message = GPErrorCode.PurchasePending.errorMsg, httpStatusCode = billingResult.responseCode)))
                        }
                        Purchase.PurchaseState.UNSPECIFIED_STATE -> {
                            purchaseCallBack?.onError(CBException(ErrorDetail(message = GPErrorCode.PurchaseUnspecified.errorMsg, httpStatusCode = billingResult.responseCode)))
                        }
                    }
                }
            }
            ITEM_ALREADY_OWNED -> {
                Log.e(TAG, "Billing response code : ITEM_ALREADY_OWNED")
                purchaseCallBack?.onError(CBException(ErrorDetail(message = GPErrorCode.ProductAlreadyOwned.errorMsg, httpStatusCode = billingResult.responseCode)))
            }
            SERVICE_DISCONNECTED -> {
                connectToBillingService()
            }
            ITEM_UNAVAILABLE -> {
                Log.e(TAG, "Billing response code : ITEM_UNAVAILABLE")
                purchaseCallBack?.onError(CBException(ErrorDetail(message = GPErrorCode.ProductUnavailable.errorMsg, httpStatusCode = billingResult.responseCode)))
            }
            USER_CANCELED ->{
                Log.e(TAG, "Billing response code  : USER_CANCELED ")
                purchaseCallBack?.onError(CBException(ErrorDetail(message = GPErrorCode.CanceledPurchase.errorMsg, httpStatusCode = billingResult.responseCode)))
            }
            ITEM_NOT_OWNED ->{
                Log.e(TAG, "Billing response code  : ITEM_NOT_OWNED ")
                purchaseCallBack?.onError(CBException(ErrorDetail(message = GPErrorCode.ProductNotOwned.errorMsg, httpStatusCode = billingResult.responseCode)))
            }
            SERVICE_TIMEOUT -> {
                Log.e(TAG, "Billing response code :SERVICE_TIMEOUT ")
                purchaseCallBack?.onError(CBException(ErrorDetail(message = GPErrorCode.PlayServiceTimeOut.errorMsg, httpStatusCode = billingResult.responseCode)))
            }
            SERVICE_UNAVAILABLE -> {
                Log.e(TAG, "Billing response code: SERVICE_UNAVAILABLE")
                purchaseCallBack?.onError(CBException(ErrorDetail(message = GPErrorCode.PlayServiceUnavailable.errorMsg, httpStatusCode = billingResult.responseCode)))
            }
            ERROR -> {
                Log.e(TAG, "Billing response code: ERROR")
                purchaseCallBack?.onError(CBException(ErrorDetail(message = GPErrorCode.UnknownError.errorMsg, httpStatusCode = billingResult.responseCode)))
            }
            DEVELOPER_ERROR -> {
                Log.e(TAG, "Billing response code: DEVELOPER_ERROR")
                purchaseCallBack?.onError(CBException(ErrorDetail(message = GPErrorCode.DeveloperError.errorMsg, httpStatusCode = billingResult.responseCode)))
            }
            BILLING_UNAVAILABLE -> {
                Log.e(TAG, "Billing response code: BILLING_UNAVAILABLE")
                purchaseCallBack?.onError(CBException(ErrorDetail(message = GPErrorCode.BillingUnavailable.errorMsg, httpStatusCode = billingResult.responseCode)))
            }
            FEATURE_NOT_SUPPORTED -> {
                Log.e(TAG, "Billing response code: FEATURE_NOT_SUPPORTED")
                purchaseCallBack?.onError(CBException(ErrorDetail(message = GPErrorCode.FeatureNotSupported.errorMsg, httpStatusCode = billingResult.responseCode)))
            }
        }
    }

    /* Acknowledge the Purchases */
    private fun acknowledgePurchase(purchase: Purchase) {
        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    try {
                        if (purchase.purchaseToken.isEmpty()){
                            Log.e(TAG, "Receipt Not Found")
                            purchaseCallBack?.onError(CBException(ErrorDetail(message = GPErrorCode.PurchaseReceiptNotFound.errorMsg, httpStatusCode = billingResult.responseCode)))
                        }else {
                            Log.i(TAG, "Google Purchase - success")
                            Log.i(TAG, "Purchase Token -${purchase.purchaseToken}")
                            validateReceipt(purchase.purchaseToken, product)
                        }

                    } catch (ex: CBException) {
                        Log.e("Error", ex.toString())
                        purchaseCallBack?.onError(CBException(ErrorDetail(message = ex.message)))
                    }
                }
            }
        }

    }

    /* Chargebee method called here to validate receipt */
    private fun validateReceipt(purchaseToken: String, product: CBProduct) {
        try{
        CBPurchase.validateReceipt(purchaseToken, product) {
            when(it) {
                is ChargebeeResult.Success -> {
                    Log.i(
                        TAG,
                        "Validate Receipt Response:  ${(it.data as CBReceiptResponse).in_app_subscription}"
                    )
                    if (it.data.in_app_subscription != null){
                        val subscriptionId = (it.data).in_app_subscription.subscription_id
                        Log.i(TAG, "Subscription ID:  $subscriptionId")
                        val subscriptionResult = (it.data).in_app_subscription
                        if (subscriptionId.isEmpty()) {
                            purchaseCallBack?.onSuccess(subscriptionResult, false)
                        } else {
                            purchaseCallBack?.onSuccess(subscriptionResult, true)
                        }
                    }else{
                        purchaseCallBack?.onError(CBException(ErrorDetail(message = GPErrorCode.PurchaseInvalid.errorMsg)))
                    }
                }
                is ChargebeeResult.Error -> {
                    Log.e(TAG, "Exception from Server - validateReceipt() :  ${it.exp.message}")
                    purchaseCallBack?.onError(it.exp)
                }
            }
        }
        }catch (exp: Exception){
            Log.e(TAG, "Exception from Server- validateReceipt() :  ${exp.message}")
            purchaseCallBack?.onError(CBException(ErrorDetail(message = exp.message)))
        }
    }

    fun queryAllPurchases(){
        billingClient.queryPurchasesAsync(
            BillingClient.SkuType.SUBS
        ) { billingResult, activeSubsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.i(TAG, "queryAllPurchases  :$activeSubsList")
            } else {
                Log.i(
                    TAG,
                    "queryAllPurchases  :${billingResult.debugMessage}"
                )
            }
        }
    }

    fun queryPurchaseHistory(){
        billingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.SUBS){ billingResult, subsHistoryList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.i(TAG, "queryPurchaseHistory  :$subsHistoryList")
            } else {
                Log.i(
                    TAG,
                    "queryPurchaseHistory  :${billingResult.debugMessage}"
                )
            }
        }
    }
    companion object {
        @JvmSynthetic
        internal var backingFieldSharedInstance: BillingClientManager? = null

        /**
         * Singleton instance of Purchases. [configure] will set this
         * @return A previously set singleton Purchases instance
         * @throws UninitializedPropertyAccessException if the shared instance has not been configured.
         */
        @JvmStatic
        var sharedInstance: BillingClientManager
            get() =
                backingFieldSharedInstance
                    ?: throw UninitializedPropertyAccessException("InstanceIssue")
            internal set(value) {
                backingFieldSharedInstance?.billingClient?.endConnection()
                backingFieldSharedInstance = value
            }
    }

}
