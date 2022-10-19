package com.chargebee.example.webcheckout

import android.net.http.SslError
import android.os.Bundle
import android.view.MenuItem
import android.webkit.*
import androidx.annotation.NonNull
import androidx.appcompat.app.ActionBar
import androidx.lifecycle.Observer
import com.chargebee.android.checkout.CheckoutListener
import com.chargebee.example.BaseActivity
import com.chargebee.example.R
import kotlinx.android.synthetic.main.activity_item.*


class WebCheckoutActivity : BaseActivity(), CheckoutListener {

    private lateinit var mWebview: WebView
   // private val checkoutURL: String = "https://sleepnumbertest-test.chargebee.com/hosted_pages/checkout?subscription_items[item_price_id][0]=Sleep-Health-Gold-USD-Monthly&subscription_items[quantity][0]=1"
    private lateinit var viewModel: CheckOutViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_checkout)

        // calling the action bar
        val actionBar: ActionBar? = supportActionBar

        // showing the back button in action bar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        mWebview = findViewById(R.id.webview)
        viewModel = CheckOutViewModel()

        viewModel.openHostedPage(this, mWebview, this)

       this.viewModel.paymentResult.observe(this, Observer {
           if (it) {
               alertNativeCheckoutSuccess("Your purchase was done successfully")
           } else {
               alertNativeCheckoutSuccess("Failed in Payment, Please try again!")
           }
       })
    }

    override fun onShowProgress() {
        showProgressDialog()
    }

    override fun onHideProgress() {
        hideProgressDialog()
    }

    // This function to handle the back button press
    override fun onOptionsItemSelected(@NonNull item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}