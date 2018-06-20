package io.imulab.heimdall

import io.reactivex.Single
import io.reactivex.subjects.SingleSubject
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.util.concurrent.atomic.AtomicBoolean

object ServerConfig {
    
    @Volatile
    private var config: JsonObject = JsonObject()
    private var once = AtomicBoolean(false)

    fun config(): JsonObject = config

    fun property(path: String, default: Any = ""): Any {
        // check for environment variable equivalent
        val envKey = path.replace(".", "_", true).toUpperCase()
        if (config.containsKey(envKey))
            return config.getValue(envKey)

        // get property from period delimited path
        val segs = path.split(".")
        var prop: JsonObject = config
        segs.forEachIndexed { index, s ->
            if (index == segs.size - 1)
                return@forEachIndexed

            if (prop.containsKey(s))
                prop = prop.getJsonObject(s)
            else
                return default
        }

        return prop.getValue(segs.last(), default)
    }

    fun propertyAsString(path: String, default: String = ""): String = property(path, default) as String

    fun propertyAsInt(path: String, default: Int = 0): Int = property(path, default) as Int

    fun load(vertx: Vertx,
             defaults: JsonObject = JsonObject(),
             yamlPaths: Set<String> = setOf(),
             envKeys: Set<String> = setOf()): Single<JsonObject> {
        val future = SingleSubject.create<JsonObject>()

        if (once.get()) {
            future.onSuccess(this.config)
            return future
        }

        once.doOnce {
            val options = ConfigRetrieverOptions()

            // combine configuration options and convert into Vert.x ConfigStoreOptions
            mutableListOf<Pair<String, Any>>().also { collector ->
                collector.add(Pair("default", defaults))
                collector.addAll(yamlPaths.map { Pair("yaml", it) })
                if (envKeys.isNotEmpty())
                    collector.add(Pair("env", envKeys))
            }.map {
                it.toConfigStoreOptions()
            }.forEach {
                options.addStore(it)
            }

            // Retrieve and cache configurations
            ConfigRetriever.create(vertx, options).getConfig { ar ->
                if (ar.failed())
                    future.onError(ar.cause())
                else {
                    this.config = ar.result()
                    future.onSuccess(this.config)
                }
            }
        }

        return future
    }

    fun reset() {
        once = AtomicBoolean(false)
    }

    private fun AtomicBoolean.doOnce(action: () -> Unit) {
        if (!this.getAndSet(true)) {
            action()
        }
    }

    private fun Pair<String, Any>.toConfigStoreOptions(): ConfigStoreOptions {
        return when (this.first) {
            "default" -> ConfigStoreOptions().also { opt ->
                opt.type = "json"
                opt.config = this.second as JsonObject
            }
            "yaml" -> ConfigStoreOptions().also { opt ->
                opt.type = "file"
                opt.format = "yaml"
                opt.config = JsonObject().put("path", this.second)
            }
            "env" -> ConfigStoreOptions().also { opt ->
                opt.type = "env"
                opt.config = JsonObject().put("keys", JsonArray((this.second as Set<*>).toList()))
            }
            else -> throw InvalidConfigurationException("unsupported config type key. ${this.first}")
        }
    }
}

fun stringProp(key: String) = ServerConfig.propertyAsString(key)

fun intProp(key: String) = ServerConfig.propertyAsInt(key)

class InvalidConfigurationException(reason: String) : RuntimeException(reason)
