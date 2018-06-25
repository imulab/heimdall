package io.imulab.heimdall.handler.endpoint

import io.imulab.heimdall.handler.DeliveryParameterHandler
import io.imulab.heimdall.handler.io.AuthorizationForm
import io.imulab.heimdall.handler.io.AuthorizationResponse
import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext
import org.apache.logging.log4j.LogManager

class AuthorizationEndpointHandler(private val minStateEntropy: Int,
                                   private val consentServiceURL: String) : Handler<RoutingContext> {

    private val logger = LogManager.getLogger(AuthorizationEndpointHandler::class.java)

    override fun handle(rc: RoutingContext) {
        println(rc.get<String>(DeliveryParameterHandler.FIELD_DELIVERY))

        // save delivery to state, if undefined, default to client's authorizationDelivery
        val form = AuthorizationForm(rc.request()).also { f ->
            f.validator().validate(minStateEntropy)
        }
        logger.debug("received form {}", form)

        AuthorizationResponse(token = "foobar", state = "12345678")
                .responseBuilder(consentServiceURL)
                .build(rc.response())
    }
}
