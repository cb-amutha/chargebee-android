package com.chargebee.android.resources

import android.util.Log
import com.chargebee.android.Chargebee
import com.chargebee.android.ErrorDetail
import com.chargebee.android.exceptions.ChargebeeResult
import com.chargebee.android.network.Auth
import com.chargebee.android.network.CBAuthenticationBody
import com.chargebee.android.repository.AuthRepository
import com.chargebee.android.responseFromServer

internal class AuthResource : BaseResource(Chargebee.baseUrl) {

    internal suspend fun authenticate(auth: Auth): ChargebeeResult<Any> {
        val authDetail = CBAuthenticationBody.fromCBAuthBody(auth)
        val response = apiClient.create(AuthRepository::class.java)
            .authenticateClient(
                Chargebee.encodedApiKey,Chargebee.platform,CatalogVersion.V2.value,auth.sKey,authDetail.toFormBody())

        Log.i(javaClass.simpleName, " Response :$response")
        return responseFromServer(
            response,
            ErrorDetail::class.java
        )
    }
    internal suspend fun authenticate(sdkKey: String): ChargebeeResult<Any> {
        val auth = Auth(sdkKey, Chargebee.applicationId, Chargebee.appName, Chargebee.channel)
        val authDetail = CBAuthenticationBody.fromCBAuthBody(auth)
        val response = apiClient.create(AuthRepository::class.java)
            .authenticateClient(
                Chargebee.encodedApiKey,Chargebee.platform,CatalogVersion.V2.value,auth.sKey,authDetail.toFormBody())

        Log.i(javaClass.simpleName, " Response :$response")
        return responseFromServer(
            response,
            ErrorDetail::class.java
        )
    }
}
