package com.acme.legacy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LegacyApp {
    private static final Logger log = LoggerFactory.getLogger(LegacyApp.class);

    public static void main(String[] args) {
        String id = args.length > 0 ? args[0] : "A-100";
        // WICHTIG: über OrderService (der den Switcher verkabelt)
        var svc = new OrderService();
        double gross = svc.sumGross(id);
        log.info("App computed gross (orderId={}): {}", id, gross);
    }
}
