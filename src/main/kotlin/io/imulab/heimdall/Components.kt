package io.imulab.heimdall

import io.imulab.heimdall.handler.*
import io.vertx.core.Handler
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BasicAuthHandler

class Components(val config: JsonObject) {

    var authEndpointDeliveryParamHandler: DeliveryParameterHandler? = null
        private set
        get() = field ?: DeliveryParameterHandler.createForAuthorizationEndpoint().also { field = it }

    var tokenEndpointDeliveryParamHandler: DeliveryParameterHandler? = null
        private set
        get() = field ?: DeliveryParameterHandler.createForTokenEndpoint().also { field = it }

    var authorizeEndpointHandler: AuthorizationEndpoint? = null
        private set
        get() = field ?: AuthorizationEndpoint(
                minStateEntropy = serviceOAuthStateEntropy!!,
                consentServiceURL = serviceConsentURL!!
        ).also { field = it }

    var consentEndpointHandler: ConsentEndpoint? = null
        private set
        get() = field ?: ConsentEndpoint().also { field = it }

    var tokenEndpointHandler: TokenEndpoint? = null
        private set
        get() = field ?: TokenEndpoint().also { field = it }

    var errorHandler: ErrorHandler? = null
        private set
        get() = field ?: ErrorHandler().also { field = it }

    var serviceHttpPort: Int? = null
        private set
        get() = field ?: config.int("service.http.port").also { field = it }

    var serviceOAuthStateEntropy: Int? = null
        private set
        get() = field ?: config.int("service.oauth.state.entropy").also { field = it }

    var serviceConsentURL: String? = null
        private set
        get() = field ?: config.string("service.oauth.consent.url").also { field = it }

    var clientService: ClientService? = null
        private set
        get() {
            if (field == null) {
                val stockData = config.prop("service.oauth.clients") as JsonArray
                val stockClients = mutableListOf<Client>()
                val stockSecrets = mutableMapOf<String, String>()

                stockData.forEach {
                    it as JsonObject
                    val id = it.getString("id")
                    val name = it.getString("name")
                    stockClients.add(Client(id, name))

                    if (it.containsKey("secret"))
                        stockSecrets[id] = it.getString("secret")
                }

                field = StockClientService(stockClients, stockSecrets)
            }

            return field
        }

    var clientAuthenticationHandler: ClientAuthenticationHandler? = null
        private set
        get() = field ?: ClientAuthenticationHandler(clientService!!, false).also { field = it }
}