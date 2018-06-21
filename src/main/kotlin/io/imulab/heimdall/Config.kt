package io.imulab.heimdall

import io.reactivex.Single
import io.reactivex.subjects.SingleSubject
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

object Config {

    fun load(vtx: Vertx, defaults: JsonObject = JsonObject(), yamlPaths: Set<String> = setOf(),
             envKeys: Set<String> = setOf()): Single<JsonObject> {
        val future = SingleSubject.create<JsonObject>()
        val options = ConfigRetrieverOptions()

        // combine configuration options and convert into Vert.x ConfigStoreOptions
        mutableListOf<Pair<String, Any>>().also { collector ->
            collector.add(Pair("default", defaults))
            collector.addAll(yamlPaths.map { Pair("yaml", it) })
            if (envKeys.isNotEmpty())
                collector.add(Pair("env", envKeys))
        }.map(this::createConfigStoreOptions).forEach { options.addStore(it) }

        // Retrieve configurations
        ConfigRetriever.create(vtx, options).getConfig { ar ->
            if (ar.failed())
                future.onError(ar.cause())
            else
                future.onSuccess(ar.result())
        }

        return future
    }

    private fun createConfigStoreOptions(kv: Pair<String, Any>): ConfigStoreOptions {
        return when (kv.first) {
            "default" -> ConfigStoreOptions().also { opt ->
                opt.type = "json"
                opt.config = kv.second as JsonObject
            }
            "yaml" -> ConfigStoreOptions().also { opt ->
                opt.type = "file"
                opt.format = "yaml"
                opt.config = JsonObject().put("path", kv.second)
            }
            "env" -> ConfigStoreOptions().also { opt ->
                opt.type = "env"
                opt.config = JsonObject().put("keys", JsonArray((kv.second as Set<*>).toList()))
            }
            else -> throw InvalidConfigurationException("unsupported config type key. ${kv.first}")
        }
    }
}