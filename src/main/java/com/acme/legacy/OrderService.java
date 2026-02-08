package com.acme.legacy;

import com.acme.legacy.sca.EnvTogglePolicy;
import com.acme.legacy.sca.LegacyOrderAdapter;
import com.acme.legacy.sca.NewOrderAdapter;
import com.acme.legacy.sca.SwitchingOrderApi;

import java.math.BigDecimal;

/**
 * Compatibility facade preserving the original FQN and API.
 */
public final class OrderService {
    private final OrderApi api;

    /**
     * Default wiring: legacy + new + env-based policy.
     */
    public OrderService() {
        // demo net repo (replace with a real source later)
        NewOrderAdapter.NetRepo repo = id -> {
            // simple mapping for demo parity with legacy
            return switch (id) {
                case "A-100", "A-102", "A-103" -> new BigDecimal("100.00");
                case "A-101" -> new BigDecimal("200.00");
                default -> BigDecimal.ZERO;
            };
        };

        // ⬇️ Option 3: per-Order Steuerfaktor (US-Fall A-102 = 1.07, sonst 1.19)
        NewOrderAdapter.TaxPolicy tax = oid ->
                "A-102".equals(oid) ? new BigDecimal("1.07") : new BigDecimal("1.19");

        this.api = new SwitchingOrderApi(
                new LegacyOrderAdapter(new OrderLegacy()),
                new NewOrderAdapter(repo, tax),
                EnvTogglePolicy.current()
        );
    }

    /**
     * Injection constructor for tests/DI.
     */
    public OrderService(OrderApi api) {
        this.api = api;
    }

    // keep the original signature
    public double sumGross(String orderId) {
        return api.sumGross(orderId);
    }
}
