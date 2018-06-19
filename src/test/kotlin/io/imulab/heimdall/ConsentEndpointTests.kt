package io.imulab.heimdall

import io.vertx.core.Vertx
import io.vertx.junit5.Timeout
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import okhttp3.HttpUrl
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.TimeUnit

@ExtendWith(VertxExtension::class)
class ConsentEndpointTests {

    @BeforeEach
    @DisplayName("Deploy server verticle for tests")
    fun setup(vtx: Vertx, tc: VertxTestContext) {
        vtx.deployVerticle(ServerVerticle::class.java.name, tc.succeeding { tc.completeNow() })
    }

    @Test
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    @DisplayName("Endpoint should successfully redirect user agent with code and state.")
    fun testSuccessfulConsent(vtx: Vertx, tc: VertxTestContext) {
        val uri = "/consent?token=xyz&state=foo"
        vtx.createHttpClient().getNow(8080, "localhost", uri) { r ->
            Assertions.assertThat(r.statusCode()).isEqualTo(302)
            val redirect = HttpUrl.parse(r.getHeader("Location"))
            Assertions.assertThat(redirect).isNotNull
            redirect!!
            Assertions.assertThat(redirect.queryParameter("code")).isNotBlank()
            Assertions.assertThat(redirect.queryParameter("state")).isNotBlank()
            tc.completeNow()
        }
    }
}