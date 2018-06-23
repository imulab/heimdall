package io.imulab.heimdall

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.subjects.CompletableSubject
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import org.apache.logging.log4j.LogManager

class ServerVerticle : AbstractVerticle() {

    private val logger = LogManager.getLogger(ServerVerticle::class.java)

    override fun start(startFuture: Future<Void>) {
        loadConfiguration().doOnSuccess{ logger.info("configuration loaded.") }
                .map { Components(it) }
                .flatMapCompletable { startHttpServer(it).doOnComplete{ logger.info("http server started.") } }
                .subscribe({
                    logger.info("service fully started.")
                    startFuture.complete()
                }, startFuture::fail)
    }

    private fun startHttpServer(components: Components): Completable {
        val c = CompletableSubject.create()
        val router = Router.router(vertx)

        router.route().handler(BodyHandler.create())

        router.route("/").handler { rc -> rc.response().end("Hello Vert.x from kotlin!") }

        router.get("/authorize").handler(components.authEndpointDeliveryParamHandler)
        router.get("/authorize").handler(components.authorizeEndpointHandler)
                .failureHandler(components.errorHandler)

        router.get("/consent").handler(components.consentEndpointHandler)
                .failureHandler(components.errorHandler)

        router.post("/oauth/token").handler(components.clientAuthenticationHandler)
        router.post("/oauth/token").handler(components.tokenEndpointDeliveryParamHandler)
        router.post("/oauth/token").handler(components.tokenEndpointHandler)
                .failureHandler(components.errorHandler)

        vertx.createHttpServer(HttpServerOptions())
                .requestHandler(router::accept)
                .listen(components.serviceHttpPort!!) { ar ->
                    if (ar.succeeded())
                        c.onComplete()
                    else
                        c.onError(ar.cause())
                }

        return c
    }

    private fun loadConfiguration(): Single<JsonObject> {
        val defaultConfig = JsonObject()
        val yamlConfigPaths = setOf("default.yaml")
        val envConfigKeys = setOf<String>()
        return Config.load(vertx, defaultConfig, yamlConfigPaths, envConfigKeys)
    }
}
