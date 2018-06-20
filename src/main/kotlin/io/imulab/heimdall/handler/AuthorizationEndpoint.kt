package io.imulab.heimdall.handler

import io.imulab.heimdall.intProp
import io.imulab.heimdall.stringProp
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.Handler
import io.vertx.core.http.HttpServerRequest
import io.vertx.ext.web.RoutingContext
import okhttp3.HttpUrl
import org.apache.logging.log4j.LogManager
import java.net.URI

object AuthorizationEndpoint : Handler<RoutingContext> {

    private val logger = LogManager.getLogger(AuthorizationEndpoint::class)

    override fun handle(rc: RoutingContext) {
        val form = rc.request().toRequestForm()
        logger.debug("received form {}", form)
        rc.response()
                .setStatusCode(HttpResponseStatus.FOUND.code())
                .putHeader("Location", RequireConsentResponse("foobar", "12345678")
                        .buildRedirectURL())
                .end()
    }

    private fun HttpServerRequest.toRequestForm(): AuthorizationForm {
        val missing = listOf(PARAM_RESPONSE_TYPE, PARAM_CLIENT_ID, PARAM_STATE).filterNot(this.params()::contains)
        if (missing.isNotEmpty())
            throw InvalidRequestException("missing required parameter: ${missing.joinToString()}.")

        val form = AuthorizationForm(
                responseType = this.getParam(PARAM_RESPONSE_TYPE),
                clientId = this.getParam(PARAM_CLIENT_ID),
                scopes = if (this.params().contains(PARAM_SCOPE))
                    this.getParam(PARAM_SCOPE).split(" ").toSet() else emptySet(),
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

        val minEntropy = intProp("service.oauth.state.entropy")
        if (form.state.length < minEntropy)
            throw InvalidRequestException("weak state entropy, minimum is $minEntropy")

        return form
    }

    private const val PARAM_RESPONSE_TYPE = "response_type"
    private const val PARAM_CLIENT_ID = "client_id"
    private const val PARAM_SCOPE = "scope"
    private const val PARAM_REDIRECT_URI = "redirect_uri"
    private const val PARAM_STATE = "state"

    private const val RESPONSE_TYPE_CODE = "code"
    private const val RESPONSE_TYPE_TOKEN = "token"
}

data class AuthorizationForm(val responseType: String,
                             val clientId: String,
                             val scopes: Set<String>,
                             var redirectURI: String,
                             val state: String)

data class RequireConsentResponse(private val token: String,
                                  private val state: String) {

    fun buildRedirectURL(): String {
        return HttpUrl.Builder().also {
            val scheme = stringProp("service.oauth.consent.scheme")
            it.scheme(if (scheme.isBlank()) "https" else scheme)

            val host = stringProp("service.oauth.consent.host")
            it.host(if (host.isBlank()) "localhost" else host)

            val port = intProp("service.oauth.consent.port")
            if (port > 0)
                it.port(port)

            it.addQueryParameter("token", token)
            it.addQueryParameter("state", state)
        }.build().toString()
    }
}
