package com.acme.legacy.sca;

import com.acme.legacy.OrderApi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** A - Adapter for the new implementation; contract expects gross totals. */
public final class NewOrderAdapter implements OrderApi {

    /** Minimal repo for the new path; returns net totals. Replace with real source later. */
    public interface NetRepo { BigDecimal netOf(String orderId); }

    /** Policy that provides a gross factor (1 + taxRate) per order. */
    public interface TaxPolicy { BigDecimal factor(String orderId); }

    private final NetRepo repo;
    private final TaxPolicy tax;

    /** Default: DE 19% if you don't pass a policy explicitly. */
    public NewOrderAdapter(NetRepo repo) {
        this(repo, id -> BigDecimal.valueOf(1.19));
    }
    public NewOrderAdapter(NetRepo repo, TaxPolicy tax) {
        this.repo = Objects.requireNonNull(repo);
        this.tax  = Objects.requireNonNull(tax);
    }

    @Override public double sumGross(String orderId) {
        BigDecimal net    = repo.netOf(orderId);
        BigDecimal factor = tax.factor(orderId);
        BigDecimal gross  = net.multiply(factor).setScale(2, RoundingMode.HALF_UP);
        return gross.doubleValue();
    }
}
