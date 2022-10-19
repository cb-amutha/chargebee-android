package com.chargebee.android.checkout

import com.chargebee.android.exceptions.CBException

interface CheckoutListener {
    fun onShowProgress()
    fun onHideProgress()
}