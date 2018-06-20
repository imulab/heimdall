package io.imulab.heimdall

import io.vertx.core.Vertx
import io.vertx.junit5.Timeout
import io.vertx.junit5.VertxTestContext
import okhttp3.HttpUrl
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class ConsentEndpointTests : ServerFunctionTests() {

    @BeforeEach
    @DisplayName("Deploy server verticle for tests")
    fun setup(vtx: Vertx, tc: VertxTestContext) {
        vtx.deployVerticle(ServerVerticle::class.java.name, tc.succeeding { tc.completeNow() })
    }

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    @DisplayName("Endpoint should successfully redirect user agent with code and state.")
    fun testSuccessfulConsent(vtx: Vertx, tc: VertxTestContext) {
        httpGet(vtx = vtx,
                uri = "/consent?token=xyz&state=foo",
                assertStatus = statusShouldBe(302),
                assertHeaders = {
                    val redirect = HttpUrl.parse(it.get("Location"))
                    Assertions.assertThat(redirect).isNotNull
                    redirect!!
                    Assertions.assertThat(redirect.queryParameter("code")).isNotBlank()
                    Assertions.assertThat(redirect.queryParameter("state")).isNotBlank()
                    tc.completeNow()
                })
    }
}