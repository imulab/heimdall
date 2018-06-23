package io.imulab.heimdall.handler

import io.imulab.heimdall.ClientAuthenticationFailedException
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.http.HttpHeaders
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.web.RoutingContext
import java.util.*

class ClientAuthenticationHandler(private val authProvider: AuthProvider,
                                  private val optional: Boolean = true) : Handler<RoutingContext> {

    override fun handle(rc: RoutingContext) {
        parseCredentials(rc, Handler { parseCred ->
            if (parseCred.failed())
                throw parseCred.cause()
            else {
                authProvider.authenticate(parseCred.result(), { auth ->
                    if (auth.failed())
                        throw auth.cause()
                    else {
                        rc.setUser(auth.result())
                        rc.next()
                    }
                })
            }
        })
    }

    private fun parseAuthorization(ctx: RoutingContext, handler: Handler<AsyncResult<String>>) {
        val request = ctx.request()
        val headerFailure = { reason: String ->
            if (optional)
                handler.handle(Future.succeededFuture(""))
            else
                handler.handle(Future.failedFuture(ClientAuthenticationFailedException(reason)))
        }

        if (!request.headers().contains(HttpHeaders.AUTHORIZATION)) {
            headerFailure("no authorization header.")
            return
        }

        val authorization = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (authorization.isBlank()) {
            headerFailure("authorization header is empty.")
            return
        }

        try {
            val idx = authorization.indexOf(' ')
            if (idx <= 0) {
                headerFailure("malformed authorization header.")
                return
            }

            if ("Basic" != authorization.substring(0, idx)) {
                headerFailure("only Basic authentication is supported.")
                return
            }

            handler.handle(Future.succeededFuture(authorization.substring(idx + 1)))
        } catch (e: RuntimeException) {
            handler.handle(Future.failedFuture(e))
        }
    }

    private fun parseCredentials(context: RoutingContext, handler: Handler<AsyncResult<JsonObject>>) {
        parseAuthorization(context, Handler { parseAuth ->
            if (parseAuth.failed()) {
                handler.handle(Future.failedFuture<JsonObject>(parseAuth.cause()))
                return@Handler
            }

            val credentials = JsonObject()
            val decoded = try {
                String(Base64.getDecoder().decode(parseAuth.result()))
            } catch (e: RuntimeException) {
                context.fail(e)
                return@Handler
            }

            val colonIdx = decoded.indexOf(":")
            if (colonIdx != -1)
                credentials
                        .put("username", decoded.substring(0, colonIdx))
                        .put("password", decoded.substring(colonIdx + 1))
            else
                credentials.put("username", decoded)

            handler.handle(Future.succeededFuture(credentials))
        })
    }
}