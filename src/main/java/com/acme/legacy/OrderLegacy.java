package com.acme.legacy;

/**
 * Legacy "god" service mixing responsibilities (I/O, logic, state).
 * Do NOT copy this style; it exists to be migrated.
 */
public class OrderLegacy implements OrderApi {
    // Global flag, controlled via -Dlegacy.log=true
    public static boolean ENABLE_LOG = Boolean.getBoolean("legacy.log");

    /** Calculates gross total for an order id using multiple legacy collaborators. */
    @Override
    public double sumGross(String orderId) {
        if (ENABLE_LOG) System.out.println("[legacy] sumGross(" + orderId + ")");
        OrderRepository repo = OrderRepository.getInstance();
        Order o = repo.find(orderId);
        if (o == null) return 0.0d;

        // Parse "net" stored as localized string (e.g., "100,00")
        double net = CurrencyUtil.parseEuro(o.netTotal);

        // Inconsistent legacy rule: if includeTax == true, we assume the stored value is already gross
        boolean assumeNetIsGross = o.includeTax;

        double result;
        if (assumeNetIsGross) {
            result = net; // already gross - legacy quirk
        } else {
            double rate = TaxCalculator.taxRate(o.country);
            result = net * (1.0d + rate);
        }

        // Arbitrary rounding using binary double (known precision issues, intentional here)
        result = Math.round(result * 100.0d) / 100.0d;

        // Side effect: write last result into a system property (terrible practice)
        System.setProperty("legacy.lastGross", String.valueOf(result));

        return result;
    }
}
