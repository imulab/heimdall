package io.imulab.heimdall.handler.io

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpServerResponse
import okhttp3.HttpUrl

data class AuthorizationResponse(var token: String, var state: String) {

    fun responseBuilder(consentServiceURL: String) = AuthorizationResponseBuilder(this, consentServiceURL)
}

class AuthorizationResponseBuilder(private val auth: AuthorizationResponse,
                                   consentServiceURL: String) {

    private var redirectionURL: HttpUrl? = null

    init {
        assert(auth.token.isNotBlank(), { "consent token should not be blank." })
        assert(auth.state.isNotBlank(), { "state should not be blank." })
        HttpUrl.parse(consentServiceURL).also { parsedURL ->
            assert(parsedURL != null, { "invalid consent service url." })
            redirectionURL = parsedURL
        }
    }

    fun build(resp: HttpServerResponse) {
        resp.also {
            it.statusCode = HttpResponseStatus.FOUND.code()
            it.putHeader(HttpHeaders.LOCATION, redirectionURL())
        }.end()
    }

    private fun redirectionURL(): String {
        return redirectionURL!!.newBuilder()
                .also {
                    it.addQueryParameter(PARAM_TOKEN, auth.token)
                    it.addQueryParameter(PARAM_STATE, auth.state)
                }.build().toString()
    }

    companion object {
        private const val PARAM_TOKEN = "token"
        private const val PARAM_STATE = "state"
    }
}
