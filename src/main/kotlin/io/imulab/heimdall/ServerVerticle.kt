package io.imulab.heimdall

import io.vertx.core.AbstractVerticle

private const val DEFAULT_PORT = 8080

class ServerVerticle : AbstractVerticle() {

    override fun start() {
        vertx.createHttpServer()
                .requestHandler{ req -> req.response().end("Hello Vert.x from kotlin!") }
                .listen(DEFAULT_PORT)
    }
}
