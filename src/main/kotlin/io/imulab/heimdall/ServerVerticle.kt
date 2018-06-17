package io.imulab.heimdall

import io.reactivex.Single
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import org.apache.logging.log4j.LogManager

private const val DEFAULT_PORT = 8080

class ServerVerticle : AbstractVerticle() {

    private val logger = LogManager.getLogger(ServerVerticle::class.java)

    override fun start() {
        loadConfiguration().subscribe { json ->
            logger.info("properties fully loaded!")
        }

        vertx.createHttpServer()
                .requestHandler{ req -> req.response().end("Hello Vert.x from kotlin!") }
                .listen(DEFAULT_PORT)
    }

    private fun loadConfiguration(): Single<JsonObject> {
        return io.vertx.reactivex.config.ConfigRetriever(ConfigRetriever.create(vertx, ConfigRetrieverOptions()
                .addStore(ConfigStoreOptions().also {
                    it.type = "file"
                    it.format = "yaml"
                    it.config = JsonObject().put("path", "app.yaml")
                })
                .addStore(ConfigStoreOptions().also {
                    it.type = "env"
                })))
                .rxGetConfig()
    }
}
