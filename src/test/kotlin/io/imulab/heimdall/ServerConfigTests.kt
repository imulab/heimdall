package io.imulab.heimdall

import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.junit5.Timeout
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.TimeUnit

@ExtendWith(VertxExtension::class)
class ServerConfigTests {

    @Test
    @Timeout(value = 30, timeUnit = TimeUnit.SECONDS)
    @DisplayName("Test loading configuration from a YAML file")
    fun testLoadYaml(vtx: Vertx, tc: VertxTestContext) {
        ServerConfig.load(vertx = vtx, yamlPaths = setOf("server-config-test.yaml"))
                .subscribe({
                    assertThat(ServerConfig.config()
                            .getJsonObject("foo")
                            .getString("bar"))
                            .isEqualTo("hello")
                    assertThat(ServerConfig.config()
                            .getJsonObject("foo")
                            .getJsonArray("baz"))
                            .contains("x", "y", "z")
                    tc.completeNow()
                }, tc::failNow)
    }

    @Test
    @Timeout(value = 30, timeUnit = TimeUnit.SECONDS)
    @DisplayName("Test loading configuration from the default source")
    fun testLoadDefault(vtx: Vertx, tc: VertxTestContext) {
        ServerConfig.load(vertx = vtx, defaults = JsonObject().put("foo", "bar"))
                .subscribe({ _ ->
                    assertThat(ServerConfig.config().getString("foo"))
                            .isEqualTo("bar")
                    tc.completeNow()
                }, tc::failNow)
    }

    @Test
    @Timeout(value = 30, timeUnit = TimeUnit.SECONDS)
    @DisplayName("Test loading configuration from environment variable. (Assuming \$HOME is set)")
    fun testLoadEnv(vtx: Vertx, tc: VertxTestContext) {
        ServerConfig.load(vertx = vtx, envKeys = setOf("HOME"))
                .subscribe({ _ ->
                    assertThat(ServerConfig.config()
                            .getString("HOME"))
                            .isNotBlank()
                    tc.completeNow()
                }, tc::failNow)
    }

    @Test
    @Timeout(value = 30, timeUnit = TimeUnit.SECONDS)
    @DisplayName("Test loading configuration twice without reloading, second time should pass.")
    fun testLoadTwice(vtx: Vertx, tc: VertxTestContext) {
        ServerConfig.load(vtx)
                .flatMap{ ServerConfig.load(vtx, yamlPaths = setOf("server-config-test.yaml")) }
                .subscribe({
                    assertThat(ServerConfig.config()).isEmpty()
                    tc.completeNow()
                }, tc::failNow)
    }

    @Test
    @Timeout(value = 30, timeUnit = TimeUnit.SECONDS)
    @DisplayName("Test overriding configuration entries")
    fun testLoadOverride(vtx: Vertx, tc: VertxTestContext) {
        ServerConfig.load(vertx = vtx,
                defaults = JsonObject().put("foo", "bar"),
                yamlPaths = setOf("server-config-test.yaml"))
                .subscribe({
                    assertThat(ServerConfig.config()
                            .getJsonObject("foo")
                            .getString("bar"))
                            .isEqualTo("hello")
                    assertThat(ServerConfig.config()
                            .getJsonObject("foo")
                            .getJsonArray("baz"))
                            .contains("x", "y", "z")
                    tc.completeNow()
                }, tc::failNow)
    }

    @Test
    @Timeout(value = 30, timeUnit = TimeUnit.SECONDS)
    @DisplayName("Test overriding configuration entries")
    fun testGetProperty(vtx: Vertx, tc: VertxTestContext) {
        ServerConfig.load(vertx = vtx,
                yamlPaths = setOf("server-config-test.yaml"))
                .subscribe({
                    assertThat(ServerConfig.property("foo.bar")).isEqualTo("hello")
                    assertThat(ServerConfig.propertyAsString("foo.bar")).isEqualTo("hello")
                    assertThat(ServerConfig.property("foo.baz") as Iterable<*>)
                            .contains("x", "y", "z")
                    tc.completeNow()
                }, tc::failNow)
    }

    @AfterEach
    fun cleanUp() {
        ServerConfig.reset()
    }
}