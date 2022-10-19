package com.chargebee.example.webcheckout

import android.content.Context
import android.util.Log
import android.webkit.WebView
import androidx.lifecycle.MutableLiveData
import com.chargebee.android.billingservice.CBCallback
import com.chargebee.android.checkout.Checkout
import com.chargebee.android.checkout.CheckoutListener
import com.chargebee.android.exceptions.CBException

class CheckOutViewModel {

    var paymentResult: MutableLiveData<Boolean> = MutableLiveData()

    fun openHostedPage(
        context: Context,
        mWebview: WebView,
        listener: CheckoutListener
    ){

        val params: List<String> = listOf("Sleep-Health-Gold-USD-Monthly", "1")

        val checkout: Checkout = Checkout(mWebview, listener)
        checkout.openHostedPage(context,params, object : CBCallback.CheckoutCallback<String> {
            override fun onCompleted(status: Boolean) {
                Log.i(javaClass.simpleName, "Your purchase has done successfully")
                Log.i(javaClass.simpleName, "Your purchase status is : $status")
                paymentResult.postValue(status)
            }

            override fun onFailed(error: CBException) {
                Log.i(javaClass.simpleName, "Error from callback : ${error.message}")
            }

            override fun onCanceled(error: CBException) {
                Log.i(javaClass.simpleName, "Error : ${error.message}")
            }

        });
    }
}