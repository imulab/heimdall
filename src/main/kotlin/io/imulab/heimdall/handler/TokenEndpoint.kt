package io.imulab.heimdall.handler

import io.vertx.core.Handler
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import java.net.URI
import java.nio.charset.StandardCharsets

class TokenEndpoint : Handler<RoutingContext> {

    override fun handle(rc: RoutingContext) {
        var delivery = rc.get<String>(DeliveryParameterHandler.FIELD_DELIVERY)
        if (delivery.isBlank())
            delivery = DeliveryParameterHandler.DELIVERY_JSON
        val form = rc.request().toForm()

        when (form) {
            is TokenByCodeForm -> handle(rc, delivery, form)
            is TokenByCredentialForm -> handle(rc, delivery, form)
            else -> throw IllegalStateException("unsupported token form.")
        }
    }

    private fun handle(rc: RoutingContext, delivery: String, form: TokenByCodeForm) {
        val response = TokenResponse(accessToken = "xyz", expiresInSeconds = 3600)
        rc.response().also {
            response.writeResponse(delivery, it)
        }.end()
    }

    private fun handle(rc: RoutingContext, delivery: String, form: TokenByCredentialForm) {
        val response = TokenResponse(accessToken = "xyz", expiresInSeconds = 3600)
        rc.response().also {
            response.writeResponse(delivery, it)
        }.end()
    }

    private fun HttpServerRequest.toForm(): Any {
        return when(this.getFormAttribute(FIELD_GRANT_TYPE)) {
            GRANT_TYPE_CODE -> this.toTokenByCodeForm()
            GRANT_TYPE_CLIENT_CREDENTIALS -> this.toTokenByCredentialForm()
            else -> throw InvalidRequestException("grant_type should be one of " +
                    "'$GRANT_TYPE_CODE' or '$GRANT_TYPE_CLIENT_CREDENTIALS'")
        }
    }

    private fun HttpServerRequest.toTokenByCodeForm(): TokenByCodeForm {
        val missing = listOf(FIELD_GRANT_TYPE, FIELD_CODE).filterNot(this.formAttributes()::contains)
        if (missing.isNotEmpty())
            throw InvalidRequestException("missing required parameter: ${missing.joinToString()}.")

        val form = TokenByCodeForm(
                grantType = this.getFormAttribute(FIELD_GRANT_TYPE),
                code = this.getFormAttribute(FIELD_CODE),
                clientId = this.optionalFormAttribute(FIELD_CLIENT_ID),
                redirectURI = this.optionalFormAttribute(FIELD_REDIRECT_URI))

        assert(form.grantType == GRANT_TYPE_CODE)

        if (form.redirectURI.isNotBlank()) {
            try {
                URI.create(form.redirectURI)
            } catch (_: IllegalArgumentException) {
                throw InvalidRequestException("redirect_uri must be a URI.")
            }
        }

        return form
    }

    private fun HttpServerRequest.toTokenByCredentialForm(): TokenByCredentialForm {
        val missing = listOf(FIELD_GRANT_TYPE).filterNot(this.formAttributes()::contains)
        if (missing.isNotEmpty())
            throw InvalidRequestException("missing required parameter: ${missing.joinToString()}.")

        val form = TokenByCredentialForm(
                grantType = this.getFormAttribute(FIELD_GRANT_TYPE),
                scopes = this.optionalFormAttribute(FIELD_SCOPE).split(" ").toSet()
        )

        assert(form.grantType == GRANT_TYPE_CLIENT_CREDENTIALS)

        return form
    }

    private fun HttpServerRequest.optionalFormAttribute(key: String, default: String = ""): String =
            if (this.formAttributes().contains(key))
                this.getFormAttribute(key)
            else
                default

    companion object {
        private const val FIELD_GRANT_TYPE = "grant_type"
        private const val FIELD_CODE = "code"
        private const val FIELD_CLIENT_ID = "client_id"
        private const val FIELD_REDIRECT_URI = "redirect_uri"
        private const val FIELD_SCOPE = "scope"

        private const val GRANT_TYPE_CODE = "authorization_code"
        private const val GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials"
    }
}

data class TokenByCodeForm(val grantType: String,
                           val code: String?,
                           val clientId: String,
                           val redirectURI: String)

data class TokenByCredentialForm(val grantType: String,
                                 val scopes: Set<String>)

data class TokenResponse(var accessToken: String = "",
                         var expiresInSeconds: Long = 0,
                         var idToken: String = "",
                         var refreshToken: String = "",
                         var tokenType: String = "") {

    fun writeResponse(delivery: String, r: HttpServerResponse): HttpServerResponse {
        val data = toJsonObject()
        when (delivery) {
            DeliveryParameterHandler.DELIVERY_FORM -> {
                val raw = data.joinToString(separator = "&") { "${it.key}=${it.value}" }
                r.putHeader("Content-Length", raw.toByteArray(StandardCharsets.UTF_8).size.toString())
                r.putHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
                r.write(raw)
            }
            DeliveryParameterHandler.DELIVERY_JSON -> {
                val raw = Json.encodePrettily(data)
                r.putHeader("Content-Length", raw.toByteArray(StandardCharsets.UTF_8).size.toString())
                r.putHeader("Content-Type", "application/json; charset=utf-8")
                r.write(raw)
            }
            else -> throw IllegalStateException("invalid delivery '$delivery'.")
        }
        return r
    }

    fun toJsonObject(): JsonObject {
        return JsonObject().also {
            if (accessToken.isNotBlank())
                it.put(FIELD_ACCESS_TOKEN, accessToken)
            if (expiresInSeconds > 0)
                it.put(FIELD_EXPIRES_IN, expiresInSeconds)
            if (idToken.isNotBlank())
                it.put(FIELD_ID_TOKEN, idToken)
            if (refreshToken.isNotBlank())
                it.put(FIELD_REFRESH_TOKEN, refreshToken)
            if (tokenType.isNotBlank())
                it.put(FIELD_TOKEN_TYPE, tokenType)
        }
    }

    private companion object {
        const val FIELD_ACCESS_TOKEN = "access_token"
        const val FIELD_EXPIRES_IN = "expires_in"
        const val FIELD_ID_TOKEN = "id_token"
        const val FIELD_REFRESH_TOKEN = "refresh_token"
        const val FIELD_TOKEN_TYPE = "token_type"
    }
}