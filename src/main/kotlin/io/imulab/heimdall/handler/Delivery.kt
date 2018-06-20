package io.imulab.heimdall.handler

import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext
import org.apache.logging.log4j.LogManager

class DeliveryParameterHandler(private val allowed: Set<String>) : Handler<RoutingContext> {

    companion object {
        const val FIELD_DELIVERY = "delivery"

        const val DELIVERY_PARAM = "param"
        const val DELIVERY_FRAGMENT = "fragment"
        const val DELIVERY_FORM = "form"
        const val DELIVERY_JSON = "json"

        fun createForAuthorizationEndpoint() : DeliveryParameterHandler =
                DeliveryParameterHandler(setOf(DELIVERY_PARAM, DELIVERY_FRAGMENT))

        fun createForTokenEndpoint() : DeliveryParameterHandler =
                DeliveryParameterHandler(setOf(DELIVERY_FORM, DELIVERY_JSON))
    }

    private val logger = LogManager.getLogger(DeliveryParameterHandler::class.java)

    override fun handle(rc: RoutingContext) {
        rc.put(FIELD_DELIVERY, "")
        if (rc.request().params().contains(FIELD_DELIVERY)) {
            val value = rc.request().getParam(FIELD_DELIVERY)
            if (allowed.contains(value))
                rc.put(FIELD_DELIVERY, value)
            else
                logger.debug("delivery method '$value' is not allowed, ignored.")
        }
        rc.next()
    }
}