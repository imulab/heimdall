package io.imulab.heimdall

import io.imulab.heimdall.handler.*
import io.vertx.core.json.JsonObject

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
}