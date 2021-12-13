package com.chargebee.example

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chargebee.android.Chargebee
import com.chargebee.android.ErrorDetail
import com.chargebee.android.billingservice.CBCallback
import com.chargebee.android.billingservice.CBPurchase
import com.chargebee.android.exceptions.CBException
import com.chargebee.android.exceptions.CBProductIDResult
import com.chargebee.android.models.Products
import com.chargebee.example.adapter.ListItemsAdapter
import com.chargebee.example.addon.AddonActivity
import com.chargebee.example.billing.BillingActivity
import com.chargebee.example.billing.BillingViewModel
import com.chargebee.example.items.ItemActivity
import com.chargebee.example.items.ItemsActivity
import com.chargebee.example.plan.PlanInJavaActivity
import com.chargebee.example.plan.PlansActivity
import com.chargebee.example.token.TokenizeActivity
import com.chargebee.example.util.CBMenu
import com.chargebee.example.util.Constants.PRODUCTS_LIST_KEY
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : BaseActivity(), ListItemsAdapter.ItemClickListener {
    private var mItemsRecyclerView: RecyclerView? = null
    private var list  = arrayListOf<String>()
    var listItemsAdapter: ListItemsAdapter? = null
    var featureList = mutableListOf<CBMenu>()
    var mContext: Context? = null
    private val TAG = "MainActivity"
    private val gson = Gson()
    private var mBillingViewModel : BillingViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mContext = this
        mBillingViewModel = BillingViewModel()
        this.mItemsRecyclerView = findViewById(R.id.rv_list_feature)
        setListAdapter()

        this.mBillingViewModel!!.error.observeForever {
            alertSuccess(Gson().fromJson<ErrorDetail>(
                it,
                ErrorDetail::class.java
            ).message)
        }
    }

    private fun setListAdapter(){
        featureList = CBMenu.values().toMutableList()
        listItemsAdapter = ListItemsAdapter(featureList, this)
        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(applicationContext)
        mItemsRecyclerView?.setLayoutManager(layoutManager)
        mItemsRecyclerView?.setItemAnimator(DefaultItemAnimator())
        mItemsRecyclerView?.setAdapter(listItemsAdapter)
    }

    override fun onItemClick(view: View?, position: Int) {
        when(CBMenu.valueOf(featureList.get(position).toString()).value){
            CBMenu.Configure.value -> {
                if (view != null) {
                    onClickConfigure(view)
                }
            }
            CBMenu.GetPlans.value -> {
                val intent = Intent(this, PlansActivity::class.java)
                startActivity(intent)
            }
            CBMenu.GetPlan.value -> {
                val intent = Intent(this, PlanInJavaActivity::class.java)
                startActivity(intent)
            }
            CBMenu.GetItems.value -> {
                val intent = Intent(this, ItemsActivity::class.java)
                startActivity(intent)
            }
            CBMenu.GetItem.value -> {
                val intent = Intent(this, ItemActivity::class.java)
                startActivity(intent)
            }
            CBMenu.GetAddOn.value -> {
                val intent = Intent(this, AddonActivity::class.java)
                startActivity(intent)
            }
            CBMenu.Tokenize.value -> {
                val intent = Intent(this, TokenizeActivity::class.java)
                startActivity(intent)
            }
            CBMenu.ProductIDs.value -> {
                val queryParam = arrayOf("100")
                CBPurchase.retrieveProductIDs(queryParam) {
                    Log.i(javaClass.simpleName, "list product ID's :  ${it}")
                    when (it) {
                        is CBProductIDResult.ProductIds -> {
                            val array = it.IDs.toTypedArray()
                            GlobalScope.launch(Dispatchers.Main) {
                                alertListProductId(array)
                            }
                        }
                        is CBProductIDResult.Error -> {
                            Log.e(TAG, " ${it.exp.message}")
                        }
                    }
                }
            }
            CBMenu.GetProducts.value -> {
                //val SUBS_SKUS = arrayListOf("merchant.pro.android", "merchant.premium.android")
                getProductIdFromCustomer()
            }
            CBMenu.SubsStatus.value -> {
                mBillingViewModel?.retrieveSubscription("1000000894110088")
            }
            else ->{

            }
        }
    }

    private fun launchProductDetailsScreen(productDetails: String) {
        val intent = Intent(this, BillingActivity::class.java)
        intent.putExtra(PRODUCTS_LIST_KEY, productDetails)
        this.startActivity(intent)
    }

    private fun onClickConfigure(view: View) {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.activity_configure, null)
        val siteNameEditText  = dialogLayout.findViewById<EditText>(R.id.etv_siteName)
        val apiKeyEditText  = dialogLayout.findViewById<EditText>(R.id.etv_apikey)
        val sdkKeyEditText  = dialogLayout.findViewById<EditText>(R.id.etv_sdkkey)
        builder.setView(dialogLayout)
        builder.setPositiveButton("Initialize") { _, i ->

            if (!TextUtils.isEmpty(siteNameEditText.text.toString()) && TextUtils.isEmpty(
                    apiKeyEditText.text.toString()
                ) && !TextUtils.isEmpty(sdkKeyEditText.text.toString())
            )
                Chargebee.configure(
                    siteNameEditText.text.toString(),
                    apiKeyEditText.text.toString(),
                    true,
                    sdkKeyEditText.text.toString()
                )
        }
        builder.show()
    }

    private fun getProductIdFromCustomer() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_input_layout)
        val input = dialog.findViewById<View>(R.id.productIdInput) as EditText
        val dialogButton = dialog.findViewById<View>(R.id.btn_ok) as Button
        dialogButton.setOnClickListener {
            val productIdList = input.text.toString().split(",")
            getProductIdList(productIdList.toCollection(ArrayList()))
            dialog.dismiss()
        }
        dialog.show()
    }
    private fun getProductIdList(productIdList : ArrayList<String>){
        CBPurchase.retrieveProducts(
            this,
            productIdList,
            object : CBCallback.ListProductsCallback<ArrayList<Products>> {
                override fun onSuccess(productDetails: ArrayList<Products>) {
                    GlobalScope.launch(Dispatchers.Main) {
                        launchProductDetailsScreen(gson.toJson(productDetails))
                    }
                }
                override fun onError(error: CBException) {
                    Log.e(TAG, "Error:  ${error.message}")
                    showDialog(error.message)
                }
            })
    }

}