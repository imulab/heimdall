package io.imulab.heimdall

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.Timeout
import io.vertx.junit5.VertxExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

/**
 * Tests for ServerConfig.
 *
 * ServerConfig is not designed for concurrent write scenario, hence we need to control the execution order
 * of the tests. In particular, any concurrent execution of tests should not be allowed. In order to achieve
 * sequential execution, we use an ReentrantLock to limit the number of parties that can modify the state of
 * ServerConfig to only one.
 *
 * @see ServerConfigTests#withLock()
 */
class ServerConfigTests {

    private val lock = ReentrantLock()

    private fun withLock(test: () -> Unit) {
        lock.lock()
        try {
            test()
        } finally {
            ServerConfig.reset()
            lock.unlock()
        }
    }

    @Test
    @Timeout(value = 30, timeUnit = TimeUnit.SECONDS)
    @DisplayName("Test loading configuration from a YAML file")
    fun testLoadYaml() {
        withLock {
            ServerConfig.load(vertx = Vertx.vertx(), yamlPaths = setOf("server-config-test.yaml")).blockingGet()
            assertThat(ServerConfig.config()
                    .getJsonObject("foo")
                    .getString("bar"))
                    .isEqualTo("hello")
            assertThat(ServerConfig.config()
                    .getJsonObject("foo")
                    .getJsonArray("baz"))
                    .contains("x", "y", "z")
        }
    }

    @Test
    @Timeout(value = 30, timeUnit = TimeUnit.SECONDS)
    @DisplayName("Test loading configuration from the default source")
    fun testLoadDefault() {
        withLock {
            ServerConfig.load(vertx = Vertx.vertx(), defaults = JsonObject().put("foo", "bar")).blockingGet()
            assertThat(ServerConfig.config().getString("foo"))
                    .isEqualTo("bar")
        }
    }

    @Test
    @Timeout(value = 30, timeUnit = TimeUnit.SECONDS)
    @DisplayName("Test loading configuration from environment variable. (Assuming \$HOME is set)")
    fun testLoadEnv() {
        withLock {
            ServerConfig.load(vertx = Vertx.vertx(), envKeys = setOf("HOME")).blockingGet()
            assertThat(ServerConfig.config()
                    .getString("HOME"))
                    .isNotBlank()
        }
    }

    @Test
    @Timeout(value = 30, timeUnit = TimeUnit.SECONDS)
    @DisplayName("Test loading configuration twice without reloading, second time should pass.")
    fun testLoadTwice() {
        withLock {
            val vtx = Vertx.vertx()
            ServerConfig.load(vtx)
                    .flatMap{ ServerConfig.load(vtx, yamlPaths = setOf("server-config-test.yaml")) }
                    .blockingGet()
            assertThat(ServerConfig.config()).isEmpty()
        }
    }

    @Test
    @Timeout(value = 30, timeUnit = TimeUnit.SECONDS)
    @DisplayName("Test overriding configuration entries")
    fun testLoadOverride() {
        withLock {
            ServerConfig.load(vertx = Vertx.vertx(),
                    defaults = JsonObject().put("foo", "bar"),
                    yamlPaths = setOf("server-config-test.yaml")).blockingGet()
            assertThat(ServerConfig.config()
                    .getJsonObject("foo")
                    .getString("bar"))
                    .isEqualTo("hello")
            assertThat(ServerConfig.config()
                    .getJsonObject("foo")
                    .getJsonArray("baz"))
                    .contains("x", "y", "z")
        }
    }

    @Test
    @Timeout(value = 30, timeUnit = TimeUnit.SECONDS)
    @DisplayName("Test overriding configuration entries")
    fun testGetProperty() {
        withLock {
            ServerConfig.load(vertx = Vertx.vertx(),
                    yamlPaths = setOf("server-config-test.yaml")).blockingGet()
            assertThat(ServerConfig.property("foo.bar")).isEqualTo("hello")
            assertThat(ServerConfig.propertyAsString("foo.bar")).isEqualTo("hello")
            assertThat(ServerConfig.property("foo.baz") as Iterable<*>)
                    .contains("x", "y", "z")
        }
    }
}
