package io.imulab.heimdall

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.Timeout
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class TokenEndpointTests : ServerFunctionTests() {

    @BeforeEach
    @DisplayName("Deploy server verticle for tests")
    fun setup(vtx: Vertx, tc: VertxTestContext) {
        vtx.deployVerticle(ServerVerticle::class.java.name, tc.succeeding { tc.completeNow() })
    }

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    @DisplayName("Endpoint should return invalid_request when supplied invalid grant type")
    fun testInvalidGrantType(vtx: Vertx, tc: VertxTestContext) {
        httpFormPost(vtx = vtx, uri = "/oauth/token",
                form = listOf(Pair("grant_type", "foo")),
                assertStatus = statusShouldBe(400)) {
            val json = JsonObject(it)
            Assertions.assertThat(json.getString("error")).isEqualTo("invalid_request")
            Assertions.assertThat(json.getString("error_description")).containsIgnoringCase("grant_type")
            tc.completeNow()
        }
    }
}