package io.imulab.heimdall.handler

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext
import okhttp3.HttpUrl

object ConsentEndpoint : Handler<RoutingContext> {

    override fun handle(rc: RoutingContext) {
        val redirectURI = "https://app.com/callback?foo=bar"

        rc.response()
                .setStatusCode(HttpResponseStatus.FOUND.code())
                .putHeader("Location", buildRedirectURL(redirectURI, "xyz", "foo"))
                .end()
    }

    private fun buildRedirectURL(base: String, code: String, state: String): String {
        val b = HttpUrl.parse(base)!!
        return HttpUrl.Builder().also { builder ->
            builder.scheme(b.scheme())
            builder.host(b.host())
            if (b.port() > 0)
                builder.port(b.port())
            b.encodedPathSegments().forEachIndexed { index, s -> builder.setEncodedPathSegment(index, s) }
            b.queryParameterNames().forEach { builder.addQueryParameter(it, b.queryParameter(it)) }
            builder.addQueryParameter("code", code)
            builder.addQueryParameter("state", state)
        }.build().toString()
    }
}

data class ConsentForm(val token: String,
                       val authorizationId: String,
                       val grantedScopes: Set<String>,
                       val state: String)