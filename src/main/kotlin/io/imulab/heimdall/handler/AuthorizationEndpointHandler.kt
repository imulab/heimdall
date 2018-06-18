package io.imulab.heimdall.endpoint

import io.imulab.heimdall.endpoint.AuthorizationEndpoint.missingMandatoryParams
import io.vertx.core.Handler
import io.vertx.core.http.HttpServerRequest
import io.vertx.ext.web.RoutingContext

object AuthorizationEndpoint : Handler<RoutingContext> {

    override fun handle(rc: RoutingContext) {

        val missingMandatoryParams = rc.request().missingMandatoryParams()
        if (missingMandatoryParams.isNotEmpty()) {
            rc.response().setStatusCode(400).end(missingMandatoryParams.toString())
            return
        }

        val form = AuthorizationForm(
                responseType = rc.request().getParam("response_type"),
                clientId = rc.request().getParam("client_id"),
                redirectURI = rc.request().getParam("redirect_uri"),
                state = rc.request().getParam("state")
        )
        println(form)
        rc.response().setStatusCode(302).putHeader("Location", "https://consent.com").end()
    }

    private fun HttpServerRequest.missingMandatoryParams() =
            listOf("response_type", "client_id", "redirect_uri")
                    .filterNot(this.params()::contains)

    private fun HttpServerRequest.toRequestForm(): AuthorizationForm {
        val required = listOf(PARAM_RESPONSE_TYPE, PARAM_CLIENT_ID, PARAM_STATE)

        val missing = required.filterNot(this.params()::contains)
        if (missing.isNotEmpty())
            throw RuntimeException()
    }

    private const val PARAM_RESPONSE_TYPE = "response_type"
    private const val PARAM_CLIENT_ID = "client_id"
    private const val PARAM_REDIRECT_URI = "redirect_uri"
    private const val PARAM_STATE = "state"
}

data class AuthorizationForm(val responseType: String,
                             val clientId: String,
                             val redirectURI: String,
                             val state: String)