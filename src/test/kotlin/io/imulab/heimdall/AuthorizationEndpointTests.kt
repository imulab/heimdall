package io.imulab.heimdall

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.junit5.Timeout
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import okhttp3.HttpUrl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.TimeUnit

@ExtendWith(VertxExtension::class)
class AuthorizationEndpointTests {

    @BeforeEach
    @DisplayName("Deploy server verticle for tests")
    fun setup(vtx: Vertx, tc: VertxTestContext) {
        vtx.deployVerticle(ServerVerticle::class.java.name, tc.succeeding { tc.completeNow() })
    }

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    @DisplayName("Endpoint should successfully redirect user agent.")
    fun testSuccessfulRedirection(vtx: Vertx, tc: VertxTestContext) {
        val uri = "/authorize?response_type=code&client_id=xyz&state=foofoofoo"
        vtx.createHttpClient().getNow(8080, "localhost", uri) { r ->
            assertThat(r.statusCode()).isEqualTo(302)
            val redirect = HttpUrl.parse(r.getHeader("Location"))
            assertThat(redirect).isNotNull
            redirect!!
            assertThat(redirect.queryParameter("token")).isNotBlank()
            assertThat(redirect.queryParameter("state")).isNotBlank()
            tc.completeNow()
        }
    }

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    @DisplayName("Endpoint should return invalid_request when supplied insufficient parameters")
    fun testInsufficientParameters(vtx: Vertx, tc: VertxTestContext) {
        this.assertWithEndpoint(vtx, "/authorize?client_id=xyz", 400) { body ->
            assertThat(body.length()).isGreaterThan(0)

            val json = JsonObject(body)
            assertThat(json.getString("error")).isEqualTo("invalid_request")
            assertThat(json.getString("error_description")).containsIgnoringCase("missing required parameter")

            tc.completeNow()
        }
    }

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    @DisplayName("Endpoint should return invalid_request when supplied invalid response type")
    fun testInvalidResponseType(vtx: Vertx, tc: VertxTestContext) {
        this.assertWithEndpoint(vtx, "/authorize?response_type=foo&client_id=xyz&state=12345678", 400) { body ->
            assertThat(body.length()).isGreaterThan(0)

            val json = JsonObject(body)
            assertThat(json.getString("error")).isEqualTo("invalid_request")
            assertThat(json.getString("error_description")).containsIgnoringCase("response_type")

            tc.completeNow()
        }
    }

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    @DisplayName("Endpoint should return invalid_request when supplied with insufficient entropy")
    fun testInsufficientEntropy(vtx: Vertx, tc: VertxTestContext) {
        this.assertWithEndpoint(vtx, "/authorize?response_type=code&client_id=xyz&state=foo", 400) { body ->
            assertThat(body.length()).isGreaterThan(0)

            val json = JsonObject(body)
            assertThat(json.getString("error")).isEqualTo("invalid_request")
            assertThat(json.getString("error_description")).containsIgnoringCase("entropy")

            tc.completeNow()
        }
    }

    private fun assertWithEndpoint(vtx: Vertx, uri: String, expectStatus: Int, assertBody: (Buffer) -> Unit) {
        vtx.createHttpClient().getNow(8080, "localhost", uri) { r ->
            assertThat(r.statusCode()).isEqualTo(expectStatus)
            r.bodyHandler(assertBody)
        }
    }
}