package io.imulab.heimdall

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.Timeout
import io.vertx.junit5.VertxTestContext
import okhttp3.HttpUrl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class AuthorizationEndpointHandlerTests : ServerFunctionTests() {

    @BeforeEach
    @DisplayName("Deploy server verticle for tests")
    fun setup(vtx: Vertx, tc: VertxTestContext) {
        vtx.deployVerticle(ServerVerticle::class.java.name, tc.succeeding { tc.completeNow() })
    }

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    @DisplayName("Endpoint should successfully redirect user agent.")
    fun testSuccessfulRedirection(vtx: Vertx, tc: VertxTestContext) {
        httpGet(vtx = vtx,
                uri = "/authorize?response_type=code&client_id=xyz&state=foofoofoo",
                assertStatus = statusShouldBe(302),
                assertHeaders = {
                    val redirect = HttpUrl.parse(it.get("Location"))
                    assertThat(redirect).isNotNull
                    redirect!!
                    assertThat(redirect.queryParameter("token")).isNotBlank()
                    assertThat(redirect.queryParameter("state")).isNotBlank()
                    tc.completeNow()
                })
    }

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    @DisplayName("Endpoint should return invalid_request when supplied insufficient parameters")
    fun testInsufficientParameters(vtx: Vertx, tc: VertxTestContext) {
        httpGet(vtx = vtx,
                uri = "/authorize?client_id=xyz",
                assertStatus = statusShouldBe(400)) {
            val json = JsonObject(it)
            assertThat(json.getString("error")).isEqualTo("invalid_request")
            assertThat(json.getString("error_description")).containsIgnoringCase("missing required parameter")
            tc.completeNow()
        }
    }

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    @DisplayName("Endpoint should return invalid_request when supplied invalid response type")
    fun testInvalidResponseType(vtx: Vertx, tc: VertxTestContext) {
        httpGet(vtx = vtx,
                uri = "/authorize?response_type=foo&client_id=xyz&state=12345678",
                assertStatus = statusShouldBe(400)) {
            val json = JsonObject(it)
            assertThat(json.getString("error")).isEqualTo("invalid_request")
            assertThat(json.getString("error_description")).containsIgnoringCase("response_type")
            tc.completeNow()
        }
    }

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    @DisplayName("Endpoint should return invalid_request when supplied with insufficient entropy")
    fun testInsufficientEntropy(vtx: Vertx, tc: VertxTestContext) {
        httpGet(vtx = vtx,
                uri = "/authorize?response_type=code&client_id=xyz&state=foo",
                assertStatus = statusShouldBe(400)) {
            val json = JsonObject(it)
            assertThat(json.getString("error")).isEqualTo("invalid_request")
            assertThat(json.getString("error_description")).containsIgnoringCase("entropy")
            tc.completeNow()
        }
    }
}