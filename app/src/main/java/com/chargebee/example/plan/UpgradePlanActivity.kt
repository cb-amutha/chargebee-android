package com.chargebee.example.plan

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import androidx.lifecycle.Observer
import com.chargebee.android.billingservice.CBCallback
import com.chargebee.android.billingservice.CBPurchase
import com.chargebee.android.exceptions.CBException
import com.chargebee.example.BaseActivity
import com.chargebee.example.R
import com.chargebee.example.billing.BillingViewModel
import com.google.android.material.textfield.TextInputEditText

class UpgradePlanActivity : BaseActivity() {

    private var mBillingViewModel : BillingViewModel? = null
    lateinit var mProductIdInput: TextInputEditText
    lateinit var mPurchaseTokenInput: TextInputEditText
    lateinit var mBuyProductButton: Button

    lateinit var context: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upgradeplan)
        context = this
        mBillingViewModel = BillingViewModel()
        mProductIdInput = findViewById(R.id.productIdInput)
        mPurchaseTokenInput = findViewById(R.id.purchaseTokenInput)
        mBuyProductButton = findViewById(R.id.buyBtn)

        this.mBillingViewModel!!.updateProductPurchaseResult.observeForever{
            hideProgressDialog()
            if (!TextUtils.isEmpty(it))
            alertSuccess(it)
        }


        mBuyProductButton.setOnClickListener{
            var productId = mProductIdInput.text.toString()
            val purchaseToken = mPurchaseTokenInput.text.toString()

            val array = arrayListOf<String>(productId)
            this.mBillingViewModel!!.updatePurchase(array, purchaseToken, context)
        }

    }

//    private fun updatePurchase(productIdList: ArrayList<String>, oldPurchaseToken: String){
//        CBPurchase.retrieveSkuProducts(
//            this,
//            productIdList, oldPurchaseToken,
//            object : CBCallback.PurchaseCallback<String> {
//
//                override fun onSuccess(subscriptionID: String, status: Boolean) {
//                    Log.e(javaClass.simpleName, " Purchase Token :$subscriptionID")
//                    alertSuccess(subscriptionID)
//                }
//
//                override fun onError(error: CBException) {
//                    Log.e(javaClass.simpleName, "Error:  ${error.message}")
//                    showDialog(error.message)
//                }
//
//            })
//    }

}