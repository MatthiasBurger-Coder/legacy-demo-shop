package com.acme.legacy.sca;

import com.acme.legacy.OrderApi;
import com.acme.legacy.OrderLegacy;

/** A - Adapter wrapping the legacy implementation behind the contract. */
public final class LegacyOrderAdapter implements OrderApi {
    private final OrderLegacy legacy;
    public LegacyOrderAdapter(OrderLegacy legacy) { this.legacy = legacy; }
    @Override public double sumGross(String orderId) { return legacy.sumGross(orderId); }
}
