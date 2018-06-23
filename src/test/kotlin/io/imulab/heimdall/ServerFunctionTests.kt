package io.imulab.heimdall

import io.vertx.core.MultiMap
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.junit5.VertxExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.extension.ExtendWith
import java.net.URLEncoder
import java.nio.charset.Charset
import java.util.*

@ExtendWith(VertxExtension::class)
abstract class ServerFunctionTests {

    open fun host() = "localhost"

    open fun port() = 8080

    fun statusShouldBe(expect: Int): (Int) -> Unit = { assertThat(it).isEqualTo(expect) }

    fun httpGet(vtx: Vertx, uri: String,
                assertStatus: (Int) -> Unit = statusShouldBe(200),
                assertHeaders: (MultiMap) -> Unit = {},
                assertBody: (Buffer) -> Unit = {}) {
        vtx.createHttpClient().getNow(port(), host(), uri) { r ->
            assertStatus(r.statusCode())
            assertHeaders(r.headers())
            r.bodyHandler(assertBody)
        }
    }

    fun httpFormPost(vtx: Vertx, uri: String,
                     basicAuth: Pair<String, String>? = null,
                     form: List<Pair<String, String>>,
                     assertStatus: (Int) -> Unit = statusShouldBe(200),
                     assertHeaders: (MultiMap) -> Unit = {},
                     assertBody: (Buffer) -> Unit = {}) {
        val req = vtx.createHttpClient().post(port(), host(), uri) { r ->
            assertStatus(r.statusCode())
            assertHeaders(r.headers())
            r.bodyHandler(assertBody)
        }
        if (basicAuth != null) {
            val encoded = "${basicAuth.first}:${basicAuth.second}".let {
                Base64.getEncoder().encodeToString(it.toByteArray())
            }
            req.putHeader("authorization", "Basic $encoded")
        }
        req.putHeader("content-type", "application/x-www-form-urlencoded")
        val body = form.joinToString(separator = "&") { "${it.first}=${it.second}" }.let {
            URLEncoder.encode(it, "UTF-8")
        }
        req.putHeader("content-length", body.toByteArray(Charset.forName("UTF-8")).size.toString())
        req.write(body)
        req.end()
    }
}
