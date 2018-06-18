package io.imulab.heimdall.handler

import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext

object TokenEndpointHandler : Handler<RoutingContext> {

    override fun handle(rc: RoutingContext) {
        rc.response().end("ok")
    }
}