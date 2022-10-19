package com.chargebee.android.checkout

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.util.Log
import android.view.View
import android.webkit.*
import androidx.annotation.RequiresApi
import com.chargebee.android.Chargebee
import com.chargebee.android.ErrorDetail
import com.chargebee.android.billingservice.CBCallback
import com.chargebee.android.exceptions.CBException
import com.chargebee.android.loggers.CBLogger


class Checkout constructor(webView: WebView, listener: CheckoutListener) {

    lateinit var listener: CheckoutListener
    lateinit var webView: WebView

    init {
        this.listener = listener
        this.webView = webView
    }

    @SuppressLint("JavascriptInterface")
    fun openHostedPage(
        context: Context, params: List<String>,
        callback: CBCallback.CheckoutCallback<String>
    ) {
        val logger = CBLogger(name = "Checkout", action = "Web checkout for subscription")

        webView.loadUrl("about:blank");
        webView.visibility = View.VISIBLE

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = (true);
        webView.settings.allowContentAccess = (true);
        webView.settings.allowFileAccess = (true);
        webView.settings.allowContentAccess = (true);
        webView.settings.databaseEnabled = (true);
        webView.webChromeClient = WebChromeClient()

        webView.webViewClient = object : WebViewClient() {

            override fun doUpdateVisitedHistory(
                view: WebView?,
                url: String?,
                isReload: Boolean
            ) {
                super.doUpdateVisitedHistory(view, url, isReload)
                Log.i(javaClass.simpleName, "URL: $url")
                if (url != null) {
                    if (url.contains("thankyou") || url.contains("thank you")) {
                        callback.onCompleted(true)
                    } else if (url.contains("succeeded")){
                        Log.i(javaClass.simpleName, "succeeded")
                        webView.stopLoading()
                    }
                }
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.i(javaClass.simpleName, "onPageStarted")
                listener.onShowProgress()
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                Log.i(javaClass.simpleName, "onPageFinished")
                listener.onHideProgress()
            }

            override fun onReceivedHttpAuthRequest(
                view: WebView, handler: HttpAuthHandler,
                host: String, realm: String
            ) {
                super.onReceivedHttpAuthRequest(view, handler, host, realm)
                // Reset the time if there is auth error
                Log.i(javaClass.simpleName, "host : $host, realm: $realm")

            }

            // To handle the certificate issue
            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                Log.
                i(javaClass.simpleName, "onReceivedSslError")
            }
            // To handle the network issue
            @RequiresApi(Build.VERSION_CODES.M)
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                Log.i(javaClass.simpleName, "onReceivedError")
                Log.i(
                    javaClass.simpleName,
                    "Error code: ${error?.errorCode} Description: ${error?.description}"
                )
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                Log.i(javaClass.simpleName, "onReceivedHttpError")
            }
        }
        var formingQueryParams: String = ""
        if (params.isNotEmpty()){
            val checkoutParamObject = CBCheckoutQueryParams.fromCheckoutParams(params)
            formingQueryParams = checkoutParamObject.toCheckoutParams().entries.joinToString(",","","").replace(",","&")
        }else{
            callback.onCanceled(
                CBException(
                error = ErrorDetail("Param is empty")
            )
            )
        }
        // load url into webview
        webView.loadUrl(Chargebee.checkoutUrl + formingQueryParams)

    }
}