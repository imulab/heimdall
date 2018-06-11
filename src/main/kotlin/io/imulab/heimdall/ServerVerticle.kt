package io.imulab.heimdall

import io.vertx.core.AbstractVerticle

class ServerVerticle : AbstractVerticle() {

    override fun start() {
        vertx.createHttpServer()
                .requestHandler{ req -> req.response().end("Hello Vert.x from kotlin!") }
                .listen(8080)
    }
}