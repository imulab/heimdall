package io.imulab.heimdall.handler

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.Handler
import io.vertx.core.http.HttpServerResponse
import io.vertx.ext.web.RoutingContext
import okhttp3.HttpUrl
import org.apache.logging.log4j.LogManager

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

/**
 * This endpoint handler simulates what a 'Consent App' would do after receiving the consent token from Heimdall.
 * Note this is a simulation and demo purpose only endpoint. Do not include this in production. The simulation bears
 * a key difference with a real 'Consent App': this will not actually ask the user for consent; rather, it would just
 * automatically grant all requested scopes.
 *
 * @author Weinan Qiu
 */
class ConsentFlowSimulationEndpoint(private val heimdallServiceUrl: String) : Handler<RoutingContext> {
    override fun handle(rc: RoutingContext) {
        val token = rc.request().getParam("token")
        val state = rc.request().getParam("state")

        logger.debug("consent simulation received token {} and state {}", token, state)

        rc.response()
                .setStatusCode(HttpResponseStatus.FOUND.code())
                .putHeader("Location", HttpUrl.parse(heimdallServiceUrl)!!.newBuilder().also {
                    it.addQueryParameter("token", "abcdefg")
                    it.addQueryParameter("state", state)
                }.build().toString())
                .end()
    }

    companion object {
        private val logger = LogManager.getLogger(ConsentFlowSimulationEndpoint::class.java)
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
