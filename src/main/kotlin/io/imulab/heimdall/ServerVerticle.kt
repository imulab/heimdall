package io.imulab.heimdall

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.subjects.CompletableSubject
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import org.apache.logging.log4j.LogManager

class ServerVerticle : AbstractVerticle() {

    private val logger = LogManager.getLogger(ServerVerticle::class.java)

    override fun start(startFuture: Future<Void>) {
        loadConfiguration()
                .doOnSuccess{ logger.info("configuration loaded.") }
                .flatMapCompletable { startHttpServer().doOnComplete{ logger.info("http server started.") } }
                .subscribe({
                    logger.info("service fully started.")
                    startFuture.complete()
                }, startFuture::fail)
    }

    private fun startHttpServer(): Completable {
        val c = CompletableSubject.create()
        val router = Router.router(vertx)

        router.route("/").handler { rc ->
            rc.response().end("Hello Vert.x from kotlin!")
        }

        vertx.createHttpServer(HttpServerOptions())
                .requestHandler(router::accept)
                .listen(intProp("service.http.port")) {
                    if (it.succeeded())
                        c.onComplete()
                    else
                        c.onError(it.cause())
                }

        return c
    }

    private fun loadConfiguration(): Single<JsonObject> {
        val defaultConfig = JsonObject()
                .put("service", JsonObject()
                        .put("http", JsonObject()
                                .put("port", DEFAULT_HTTP_PORT)
                        )
                )
        val yamlConfigPaths = setOf(CONFIG_FILE)
        val envConfigKeys = setOf<String>()

        return ServerConfig.load(vertx, defaultConfig, yamlConfigPaths, envConfigKeys)
    }
}

private const val DEFAULT_HTTP_PORT = 8080
private const val CONFIG_FILE = "app.yaml"