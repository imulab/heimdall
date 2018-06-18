package io.imulab.heimdall.endpoint

import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext

object TokenEndpoint : Handler<RoutingContext> {

    override fun handle(rc: RoutingContext) {
        rc.response().end("ok")
    }
}