package io.imulab.heimdall.handler

import io.imulab.heimdall.intProp
import io.vertx.core.Handler
import io.vertx.core.http.HttpServerRequest
import io.vertx.ext.web.RoutingContext
import java.net.URI

object AuthorizationEndpoint : Handler<RoutingContext> {

    override fun handle(rc: RoutingContext) {
        val form = rc.request().toRequestForm()
        println(form)
        rc.response().setStatusCode(302).putHeader("Location", "https://consent.com").end()
    }

    private fun HttpServerRequest.toRequestForm(): AuthorizationForm {
        val missing = listOf(PARAM_RESPONSE_TYPE, PARAM_CLIENT_ID, PARAM_STATE).filterNot(this.params()::contains)
        if (missing.isNotEmpty())
            throw InvalidRequestException("missing required parameter: ${missing.joinToString()}.")

        val form = AuthorizationForm(
                responseType = this.getParam(PARAM_RESPONSE_TYPE),
                clientId = this.getParam(PARAM_CLIENT_ID),
                scopes = if (this.params().contains(PARAM_SCOPE)) this.getParam(PARAM_SCOPE).split(" ").toSet() else emptySet(),
                redirectURI = if (this.params().contains(PARAM_REDIRECT_URI)) this.getParam(PARAM_REDIRECT_URI) else "",
                state = this.getParam(PARAM_STATE))

        if (!listOf(RESPONSE_TYPE_CODE, RESPONSE_TYPE_TOKEN).contains(form.responseType))
            throw InvalidRequestException("response_type must be one of 'code' or 'token'.")

        if (form.redirectURI.isNotBlank()) {
            try {
                URI.create(form.redirectURI)
            } catch (_: IllegalArgumentException) {
                throw InvalidRequestException("redirect_uri must be a URI.")
            }
        }

        val minEntropy = intProp("service.oauth.stateEntropy")
        if (form.state.length < minEntropy)
            throw InvalidRequestException("weak state entropy, minimum is $minEntropy")

        return form
    }

    private const val PARAM_RESPONSE_TYPE = "response_type"
    private const val PARAM_CLIENT_ID = "client_id"
    private const val PARAM_SCOPE = "scope"
    private const val PARAM_REDIRECT_URI = "redirect_uri"
    private const val PARAM_STATE = "state"

    const val RESPONSE_TYPE_CODE = "code"
    const val RESPONSE_TYPE_TOKEN = "token"
}

data class AuthorizationForm(val responseType: String,
                             val clientId: String,
                             val scopes: Set<String>,
                             var redirectURI: String,
                             val state: String)