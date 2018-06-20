package io.imulab.heimdall

import io.vertx.core.Vertx
import io.vertx.junit5.Timeout
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class ServerVerticleTests : ServerFunctionTests() {

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
        httpGet(vtx = vtx,
                uri = "/") {
            assertThat(it.length()).isGreaterThan(0)
            tc.completeNow()
        }
    }
}