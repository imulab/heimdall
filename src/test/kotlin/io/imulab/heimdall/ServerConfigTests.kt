package io.imulab.heimdall

import io.vertx.core.Vertx
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
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
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
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    @DisplayName("Test loading configuration from environment variable. (Assuming \$HOME is set)")
    fun testLoadEnv(vtx: Vertx, tc: VertxTestContext) {
        ServerConfig.load(vertx = vtx, envKeys = setOf("HOME"))
                .subscribe({
                    assertThat(ServerConfig.config()
                            .getString("HOME"))
                            .isNotBlank()
                    tc.completeNow()
                }, tc::failNow)
    }

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    fun testLoadTwice(vtx: Vertx, tc: VertxTestContext) {
        ServerConfig.load(vtx)
                .andThen(ServerConfig.load(vtx, yamlPaths = setOf("server-config-test.yaml")))
                .subscribe({
                    assertThat(ServerConfig.config()).isEmpty()
                    tc.completeNow()
                }, tc::failNow)
    }

    @AfterEach
    fun cleanUp() {
        ServerConfig.reset()
    }
}