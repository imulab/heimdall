package io.imulab.heimdall.handler

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.Handler
import io.vertx.core.http.HttpServerResponse
import io.vertx.ext.web.RoutingContext
import okhttp3.HttpUrl

class ConsentEndpoint : Handler<RoutingContext> {

    override fun handle(rc: RoutingContext) {
        // respect delivery from saved state only
        val delivery = DeliveryParameterHandler.DELIVERY_PARAM
        val redirectURI = "https://app.com/callback?foo=bar"
        var consentResponse: ConsentResponse = CodeGrantResponse(redirectURI, "xyz", "foo")
        rc.response()
                .setStatusCode(HttpResponseStatus.FOUND.code())
                .also { consentResponse.writeResponse(delivery, it) }
                .end()
    }
}

data class ConsentForm(val token: String,
                       val authorizationId: String,
                       val grantedScopes: Set<String>,
                       val state: String)

interface ConsentResponse {
    fun writeResponse(delivery: String, r: HttpServerResponse): HttpServerResponse
}

data class CodeGrantResponse(private val redirectURI: String,
                             private val code: String,
                             private val state: String) : ConsentResponse {

    override fun writeResponse(delivery: String, r: HttpServerResponse): HttpServerResponse {
        return r.putHeader("Location", HttpUrl.parse(redirectURI)!!.newBuilder().also { builder ->
            val kv = listOf(Pair("code", code), Pair("state", state))
            when (delivery) {
                DeliveryParameterHandler.DELIVERY_PARAM -> kv.forEach {
                    builder.addQueryParameter(it.first, it.second)
                }
                DeliveryParameterHandler.DELIVERY_FRAGMENT -> builder.fragment(kv.joinToString("&") {
                    "${it.first}=${it.second}"
                })
                else -> throw IllegalStateException("invalid delivery '$delivery'.")
            }
        }.build().toString())
    }
}

data class ImplicitGrantResponse(private val redirectURI: String,
                                 var accessToken: String = "",
                                 var expiresInSeconds: Long = 0,
                                 var tokenType: String = "",
                                 var state: String = "",
                                 var scopes: Set<String> = emptySet()) : ConsentResponse {

    override fun writeResponse(delivery: String, r: HttpServerResponse): HttpServerResponse {
        return r.putHeader("Location", HttpUrl.parse(redirectURI)!!.newBuilder().also { builder ->
            val kv = mutableListOf<Pair<String, String>>().also {
                it.add(Pair("access_token", accessToken))
                it.add(Pair("expires_in", expiresInSeconds.toString()))
                it.add(Pair("token_type", tokenType))
                it.add(Pair("state", state))
                if (scopes.isNotEmpty())
                    it.add(Pair("scope", scopes.joinToString(separator = " ")))
            }
            when (delivery) {
                DeliveryParameterHandler.DELIVERY_PARAM -> kv.forEach {
                    builder.addQueryParameter(it.first, it.second)
                }
                DeliveryParameterHandler.DELIVERY_FRAGMENT -> builder.fragment(kv.joinToString("&") {
                    "${it.first}=${it.second}"
                })
                else -> throw IllegalStateException("invalid delivery '$delivery'.")
            }
        }.build().toString())
    }
}
