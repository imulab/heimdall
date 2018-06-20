package io.imulab.heimdall

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.junit5.Timeout
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

@ExtendWith(VertxExtension::class)
class TokenEndpointTests {

    @BeforeEach
    @DisplayName("Deploy server verticle for tests")
    fun setup(vtx: Vertx, tc: VertxTestContext) {
        vtx.deployVerticle(ServerVerticle::class.java.name, tc.succeeding { tc.completeNow() })
    }

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    @DisplayName("Endpoint should return invalid_request when supplied invalid grant type")
    fun testInvalidGrantType(vtx: Vertx, tc: VertxTestContext) {
        assertWithFormPost(vtx, "/oauth/token", listOf(Pair("grant_type", "foo")), 400) { body ->
            Assertions.assertThat(body.length()).isGreaterThan(0)

            val json = JsonObject(body)
            Assertions.assertThat(json.getString("error")).isEqualTo("invalid_request")
            Assertions.assertThat(json.getString("error_description")).containsIgnoringCase("grant_type")

            tc.completeNow()
        }
    }

    private fun assertWithFormPost(vtx: Vertx, uri: String, form: List<Pair<String, String>>, expectStatus: Int, assertBody: (Buffer) -> Unit) {
        val req = vtx.createHttpClient().post(8080, "localhost", uri) { r ->
            Assertions.assertThat(r.statusCode()).isEqualTo(expectStatus)
            r.bodyHandler(assertBody)
        }
        req.putHeader("content-type", "application/x-www-form-urlencoded")
        val body = form.joinToString(separator = "&") { "${it.first}=${it.second}" }
        req.putHeader("content-length", body.toByteArray(Charset.forName("UTF-8")).size.toString())
        req.write(body)
        req.end()
    }
}