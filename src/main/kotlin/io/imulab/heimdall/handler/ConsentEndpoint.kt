package io.imulab.heimdall.handler

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.Handler
import io.vertx.core.http.HttpServerResponse
import io.vertx.ext.web.RoutingContext
import okhttp3.HttpUrl

object ConsentEndpoint : Handler<RoutingContext> {

    override fun handle(rc: RoutingContext) {
        val redirectURI = "https://app.com/callback?foo=bar"
        var consentResponse: ConsentResponse = CodeGrantResponse(redirectURI, "xyz", "foo")
        rc.response()
                .setStatusCode(HttpResponseStatus.FOUND.code())
                .also { consentResponse.writeResponse(it) }
                .end()
    }
}

data class ConsentForm(val token: String,
                       val authorizationId: String,
                       val grantedScopes: Set<String>,
                       val state: String)

interface ConsentResponse {
    fun writeResponse(r: HttpServerResponse): HttpServerResponse
}

data class CodeGrantResponse(private val redirectURI: String,
                             private val code: String,
                             private val state: String) : ConsentResponse {

    override fun writeResponse(r: HttpServerResponse): HttpServerResponse {
        r.putHeader("Location", buildRedirectURL())
        return r
    }

    private fun buildRedirectURL(): String {
        return HttpUrl.parse(redirectURI)!!.newBuilder().also {
            it.addQueryParameter("code", code)
            it.addQueryParameter("state", state)
        }.build().toString()
    }
}

data class ImplicitGrantResponse(private val redirectURI: String,
                                 var accessToken: String = "",
                                 var expiresInSeconds: Long = 0,
                                 var tokenType: String = "",
                                 var state: String = "",
                                 var scopes: Set<String> = emptySet()) : ConsentResponse {

    override fun writeResponse(r: HttpServerResponse): HttpServerResponse {
        r.putHeader("Location", buildRedirectURL())
        return r
    }

    private fun buildRedirectURL(): String {
        return HttpUrl.parse(redirectURI)!!.newBuilder().also {
            it.addQueryParameter("access_token", accessToken)
            it.addQueryParameter("expires_in", expiresInSeconds.toString())
            it.addQueryParameter("token_type", tokenType)
            it.addQueryParameter("state", state)
            if (scopes.isNotEmpty())
                it.addQueryParameter("scope", scopes.joinToString(separator = " "))
        }.build().toString()
    }
}