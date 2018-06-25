package io.imulab.heimdall.handler.io

import io.imulab.heimdall.handler.InvalidRequestException
import io.vertx.core.http.HttpServerRequest
import java.net.URI

data class AuthorizationForm(private val req: HttpServerRequest) {

    val responseType: String
    val clientId: String
    val scopes: Set<String>
    val redirectURI: String
    val state: String

    init {
        req.assertRequiredParams()

        // initialize properties
        responseType = req.getParam(PARAM_RESPONSE_TYPE)
        clientId = req.getParam(PARAM_CLIENT_ID)
        scopes = if (req.params().contains(PARAM_SCOPE))
            req.getParam(PARAM_SCOPE).split(SPACE).toSet()
        else
            emptySet()
        redirectURI = if (req.params().contains(PARAM_REDIRECT_URI))
            req.getParam(PARAM_REDIRECT_URI)
        else
            ""
        state = req.getParam(PARAM_STATE)
    }

    fun validator() = AuthorizationFormValidator(form = this)

    private fun HttpServerRequest.assertRequiredParams() {
        val missing = REQUIRED_PARAMS.filterNot(this.params()::contains)
        if (missing.isNotEmpty())
            throw InvalidRequestException("missing required parameter: ${missing.joinToString()}.")
    }

    companion object {
        private const val PARAM_RESPONSE_TYPE = "response_type"
        private const val PARAM_CLIENT_ID = "client_id"
        private const val PARAM_SCOPE = "scope"
        private const val PARAM_REDIRECT_URI = "redirect_uri"
        private const val PARAM_STATE = "state"

        private const val SPACE = " "

        private val REQUIRED_PARAMS = listOf(PARAM_RESPONSE_TYPE, PARAM_CLIENT_ID, PARAM_STATE)
    }
}

class AuthorizationFormValidator(private val form: AuthorizationForm) {

    fun validate(minStateEntropy: Int) {
        this.assertValidResponseType()
        this.assertValidRedirectURIIfProvided()
        this.assertMinimumStateEntropy(minStateEntropy)
    }

    private fun assertValidResponseType() {
        when (form.responseType) {
            RESPONSE_TYPE_CODE,
            RESPONSE_TYPE_TOKEN -> return
            else -> throw InvalidRequestException("response_type must be one of 'code' or 'token'.")
        }
    }

    private fun assertValidRedirectURIIfProvided() {
        if (form.redirectURI.isNotBlank()) {
            try {
                URI.create(form.redirectURI)
            } catch (_: IllegalArgumentException) {
                throw InvalidRequestException("redirect_uri must be a URI.")
            }
        }
    }

    private fun assertMinimumStateEntropy(minEntropy: Int) {
        if (form.state.length < minEntropy)
            throw InvalidRequestException("weak state entropy, minimum is $minEntropy")
    }

    companion object {
        private const val RESPONSE_TYPE_CODE = "code"
        private const val RESPONSE_TYPE_TOKEN = "token"
    }
}