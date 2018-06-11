package io.imulab.heimdall

import io.vertx.core.Vertx
import io.vertx.junit5.Timeout
import org.assertj.core.api.Assertions.*
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.TimeUnit

@ExtendWith(VertxExtension::class)
class ServerVerticleTests {

    @BeforeEach
    @DisplayName("Deploy server verticle for tests")
    fun setup(vtx: Vertx, tc: VertxTestContext) {
        vtx.deployVerticle(ServerVerticle::class.java.name, tc.succeeding { tc.completeNow() })
    }

    @AfterEach
    @DisplayName("Check verticle is still alive before tearing down")
    fun checkAliveAndClean(vtx: Vertx, tc: VertxTestContext) {
        assertThat(vtx.deploymentIDs())
                .isNotEmpty
                .hasSize(1)
        vtx.close(tc.succeeding { tc.completeNow() })
    }

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    @DisplayName("Server should have started")
    fun testServerStarted(vtx: Vertx, tc: VertxTestContext) {
        vtx.createHttpClient().getNow(8080, "localhost", "/") { r ->
            assertThat(r.statusCode()).isEqualTo(200)
            r.bodyHandler { body ->
                assertThat(body.length()).isGreaterThan(0)
                tc.completeNow()
            }
        }
    }
}