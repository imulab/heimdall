package io.imulab.heimdall

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.MongoClient
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@Tag("integration")
@ExtendWith(VertxExtension::class)
class MongoLivelinessIntegrationTests {

    private lateinit var mongo: MongoClient

    @BeforeEach
    fun setup(vtx: Vertx, tc: VertxTestContext) {
        println(System.getProperty("TEST_MONGO_URI", "mongodb://localhost:32770"))
        mongo = MongoClient.createShared(vtx, JsonObject().put("connection_string",
                System.getProperty("TEST_MONGO_URI", "mongodb://localhost:32770")))
        tc.completeNow()
    }

    @Test
    fun mongoDBIsAlive(vtx: Vertx, tc: VertxTestContext) {
        mongo.runCommand("ping", JsonObject().put("ping", 1), tc.succeeding {
            assertThat(it.getDouble("ok")).isEqualTo(1.0)
            tc.completeNow()
        })
    }
}