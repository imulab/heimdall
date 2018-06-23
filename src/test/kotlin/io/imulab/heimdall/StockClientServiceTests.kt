package io.imulab.heimdall

import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.assertj.core.api.Assertions.*

@ExtendWith(VertxExtension::class)
class StockClientServiceTests {

    @Test
    fun testGetClient(vtx: Vertx, tc: VertxTestContext) {
        val clientService = StockClientService(
                clients = listOf(
                        Client("one", "one")
                ),
                secrets = emptyMap()
        )

        clientService.getClient("one", Handler { ar ->
            try {
                assertThat(ar.succeeded()).isTrue()
                assertThat(ar.cause()).isNull()
                assertThat(ar.result().id).isEqualTo("one")
                assertThat(ar.result().name).isEqualTo("one")
            } finally {
                tc.completeNow()
            }
        })
    }

    @Test
    fun testClientNotFound(vtx: Vertx, tc: VertxTestContext) {
        val clientService = StockClientService(
                clients = emptyList(),
                secrets = emptyMap()
        )

        clientService.getClient("one", Handler { ar ->
            try {
                assertThat(ar.succeeded()).isFalse()
                assertThat(ar.cause()).isInstanceOf(ClientNotFoundException::class.java)
            } finally {
                tc.completeNow()
            }
        })
    }

    @Test
    fun testAuthenticateClientSuccess(vtx: Vertx, tc: VertxTestContext) {
        val clientService = StockClientService(
                clients = listOf(
                        Client("one", "one")
                ),
                secrets = mapOf("one" to "pwd")
        )

        val credential = JsonObject()
                .put("username", "one")
                .put("password", "pwd")

        clientService.authenticate(credential, { ar ->
            try {
                assertThat(ar.succeeded()).isTrue()
                assertThat(ar.cause()).isNull()
                assertThat(ar.result()).isInstanceOf(Client::class.java)
            } finally {
                tc.completeNow()
            }
        })
    }

    @Test
    fun testAuthenticateClientFailWithWrongCredential(vtx: Vertx, tc: VertxTestContext) {
        val clientService = StockClientService(
                clients = listOf(
                        Client("one", "one")
                ),
                secrets = mapOf("one" to "pwd")
        )

        val credential = JsonObject()
                .put("username", "one")
                .put("password", "foo")

        clientService.authenticate(credential, { ar ->
            try {
                assertThat(ar.succeeded()).isFalse()
                assertThat(ar.cause()).isInstanceOf(ClientAuthenticationFailedException::class.java)
            } finally {
                tc.completeNow()
            }
        })
    }

    @Test
    fun testAuthenticateClientFailWithNoClient(vtx: Vertx, tc: VertxTestContext) {
        val clientService = StockClientService(
                clients = listOf(
                        Client("one", "one")
                ),
                secrets = mapOf("one" to "pwd")
        )

        val credential = JsonObject()
                .put("username", "foo")
                .put("password", "bar")

        clientService.authenticate(credential, { ar ->
            try {
                assertThat(ar.succeeded()).isFalse()
                assertThat(ar.cause()).isInstanceOf(ClientAuthenticationFailedException::class.java)
            } finally {
                tc.completeNow()
            }
        })
    }
}