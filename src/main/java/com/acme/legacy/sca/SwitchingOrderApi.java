package com.acme.legacy.sca;

import com.acme.legacy.OrderApi;
import com.acme.logging.Traceable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * S - Switcher: routes to legacy/new and fails back to legacy on errors.
 */
@Traceable(feature = "order.sumGross") // <— Aspect controls corrId/feature/duration
public final class SwitchingOrderApi implements OrderApi {
    private static final Logger log = LoggerFactory.getLogger(SwitchingOrderApi.class);
    private final OrderApi legacy, modern;
    private final TogglePolicy policy;

    public SwitchingOrderApi(OrderApi legacy, OrderApi modern, TogglePolicy policy) {
        this.legacy = legacy;
        this.modern = modern;
        this.policy = policy;
    }

    @Override
    public double sumGross(String orderId) {
        // Do NOT set corrId/feature here; the aspect will.
        try {
            if (policy.newEnabled() && policy.routePredicate().test(orderId)) {
                MDC.put("route", "new");
                log.info("sumGross routed to modern (orderId={})", orderId);
                return modern.sumGross(orderId);
            } else {
                MDC.put("route", "legacy");
                log.info("sumGross routed to legacy (orderId={})", orderId);
                return legacy.sumGross(orderId);
            }
        } catch (RuntimeException ex) {
            MDC.put("route", "fallback");
            log.warn("sumGross fallback to legacy (orderId={})", orderId, ex);
            return legacy.sumGross(orderId);
        }
    }
}
