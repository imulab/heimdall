package io.imulab.heimdall

import io.reactivex.Completable
import io.reactivex.subjects.CompletableSubject
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.util.concurrent.atomic.AtomicBoolean

object ServerConfig {

    private var once = AtomicBoolean(false)
    private var config: JsonObject = JsonObject()

    private fun AtomicBoolean.doOnce(action: () -> Unit) {
        if (!this.getAndSet(true)) {
            action()
        }
    }

    fun config(): JsonObject = config

    fun load(vertx: Vertx, defaults: JsonObject = JsonObject(), yamlPaths: Set<String> = setOf(), envKeys: Set<String> = setOf()): Completable {
        val completable = CompletableSubject.create()
        if (once.get()) {
            return completable.also { it.onComplete() }
        }

        once.doOnce {
            val options = ConfigRetrieverOptions()

            // combine configuration options and convert into Vert.x ConfigStoreOptions
            mutableListOf<Pair<String, Any>>().also { collector ->
                collector.add(Pair("default", defaults))
                collector.addAll(yamlPaths.map { Pair("yaml", it) })
                if (envKeys.isNotEmpty())
                    collector.add(Pair("env", envKeys))
            }.map { cfg ->
                when (cfg.first) {
                    "default" -> ConfigStoreOptions().also { opt ->
                        opt.type = "json"
                        opt.config = cfg.second as JsonObject
                    }
                    "yaml" -> ConfigStoreOptions().also { opt ->
                        opt.type = "file"
                        opt.format = "yaml"
                        opt.config = JsonObject().put("path", cfg.second)
                    }
                    "env" -> ConfigStoreOptions().also { opt ->
                        opt.type = "env"
                        opt.config = JsonObject().put("keys", JsonArray((cfg.second as Set<*>).toList()))
                    }
                    else -> throw InvalidConfigurationException("unsupported config type key. ${cfg.first}")
                }
            }.forEach { options.addStore(it) }

            // Retrieve and cache configurations
            ConfigRetriever.create(vertx, options).getConfig { ar ->
                if (ar.failed())
                    completable.onError(ar.cause())
                else {
                    this.config = ar.result()
                    completable.onComplete()
                }
            }
        }

        return completable
    }

    fun reset() {
        once = AtomicBoolean(false)
    }
}

class InvalidConfigurationException(reason: String) : RuntimeException(reason)