package io.imulab.heimdall.handler

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.Handler
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import org.apache.logging.log4j.LogManager

object ErrorHandler : Handler<RoutingContext> {

    private const val FIELD_ERROR = "error"
    private const val FIELD_ERROR_DESCRIPTION = "error_description"

    private val logger = LogManager.getLogger(ErrorHandler::class.java)

    override fun handle(ctx: RoutingContext) {
        val f = ctx.failure()
        when (f) {
            is ApiError ->
                ctx.response()
                        .putHeader("Content-Type", "application/json; charset=utf-8")
                        .setStatusCode(f.statusCode())
                        .end(Json.encodePrettily(JsonObject()
                                .put(FIELD_ERROR, f.error())
                                .put(FIELD_ERROR_DESCRIPTION, f.message)))
            else -> {
                logger.error("error handler received unknown error", f)
                ctx.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end("unknown_error")
            }
        }
    }
}

interface ApiError {
    fun statusCode(): Int
    fun error(): String
}

class InvalidRequestException(reason: String): RuntimeException(reason), ApiError {
    override fun statusCode(): Int = 400
    override fun error(): String = "invalid_request"
}
