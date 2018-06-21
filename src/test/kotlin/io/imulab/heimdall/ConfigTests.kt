package io.imulab.heimdall

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class ConfigTests {

    @Test
    fun testLoadYaml(vtx: Vertx, tc: VertxTestContext) {
        Config.load(vtx = vtx, yamlPaths = setOf("server-config-test.yaml")).subscribe { json, e ->
            try {
                assertThat(e).isNull()
                assertThat(json.getJsonObject("foo").getString("bar")).isEqualTo("hello")
                assertThat(json.getJsonObject("foo").getJsonArray("baz")).contains("x", "y", "z")
            } finally {
                tc.completeNow()
            }
        }
    }

    @Test
    fun testLoadDefault(vtx: Vertx, tc: VertxTestContext) {
        Config.load(vtx = vtx, defaults = JsonObject().put("foo", "bar")).subscribe { json, e ->
            try {
                assertThat(e).isNull()
                assertThat(json.getString("foo")).isEqualTo("bar")
            } finally {
                tc.completeNow()
            }
        }
    }

    @Test
    fun testLoadEnv(vtx: Vertx, tc: VertxTestContext) {
        Config.load(vtx = vtx, envKeys = setOf("HOME")).subscribe { json, e ->
            try {
                assertThat(e).isNull()
                assertThat(json.getString("HOME")).isNotBlank()
            } finally {
                tc.completeNow()
            }
        }
    }

    @Test
    fun testLoadOverride(vtx: Vertx, tc: VertxTestContext) {
        Config.load(vtx = vtx, defaults = JsonObject().put("foo", "bar"),
                yamlPaths = setOf("server-config-test.yaml")).subscribe { json, e ->
            try {
                assertThat(e).isNull()
                assertThat(json.getJsonObject("foo").getString("bar")).isEqualTo("hello")
                assertThat(json.getJsonObject("foo").getJsonArray("baz")).contains("x", "y", "z")
            } finally {
                tc.completeNow()
            }
        }
    }
}
