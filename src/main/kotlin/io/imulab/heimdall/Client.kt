package io.imulab.heimdall

import io.imulab.heimdall.handler.ApiError
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User
import java.util.concurrent.ConcurrentHashMap

data class Client(val id: String,
                  val name: String) : User {

    override fun clearCache(): User { return this }

    override fun setAuthProvider(authProvider: AuthProvider?) {}

    override fun isAuthorized(authority: String?, resultHandler: Handler<AsyncResult<Boolean>>?): User {
        resultHandler?.handle(Future.succeededFuture(true))
        return this
    }

    override fun principal(): JsonObject {
        return JsonObject().also {
            it.put("id", this.id)
        }
    }
}

interface ClientService : AuthProvider {
    fun getClient(id: String, resultHandler: Handler<AsyncResult<Client>>?)
}

class StockClientService(clients: List<Client>,
                         private val secrets: Map<String, String>) : ClientService {

    private val stock = ConcurrentHashMap<String, Client>()

    init {
        clients.forEach { stock[it.id] = it }
    }

    override fun getClient(id: String, resultHandler: Handler<AsyncResult<Client>>?) {
        if (resultHandler == null)
            return

        if (stock.containsKey(id))
            resultHandler.handle(Future.succeededFuture(stock.getValue(id)))
        else
            resultHandler.handle(Future.failedFuture(ClientNotFoundException(JsonObject().put("id", id))))
    }

    override fun authenticate(authInfo: JsonObject?, resultHandler: Handler<AsyncResult<User>>?) {
        if (resultHandler == null)
            return
        else if (authInfo == null) {
            resultHandler.handle(Future.failedFuture(ClientNotFoundException(JsonObject().put("id", "?"))))
            return
        }

        val id = authInfo.getString("username")
        val secret = authInfo.getString("password")

        getClient(id, Handler { ar ->
            if (ar.failed())
                resultHandler.handle(Future.failedFuture(
                        ClientAuthenticationFailedException(JsonObject().put("cause", ar.cause().message))))
            else {
                if (!checkSecret(id, secret))
                    resultHandler.handle(Future.failedFuture(
                            ClientAuthenticationFailedException(JsonObject().put("id", id))))
                else
                    resultHandler.handle(Future.succeededFuture(ar.result()))
            }
        })
    }

    private fun checkSecret(id: String, secret: String): Boolean =
            this.secrets[id] == secret
}

class ClientNotFoundException(detail: JsonObject) :
        RuntimeException("no client exists by criteria '${Json.encode(detail)}'")

class ClientAuthenticationFailedException(reason: String) : RuntimeException(reason), ApiError {
    constructor(detail: JsonObject) : this("invalid client credential: '${Json.encode(detail)}'")

    override fun statusCode(): Int = HttpResponseStatus.UNAUTHORIZED.code()

    override fun error(): String = "invalid_credential"
}
