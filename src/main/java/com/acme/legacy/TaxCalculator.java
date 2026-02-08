package com.acme.legacy;

/** Static tax calculation using system properties; not thread-safe and not pure (intentional). */
public class TaxCalculator {

    public static double taxRate(String country) {
        String c = country == null ? "" : country.toUpperCase();
        if ("DE".equals(c)) {
            String p = System.getProperty("tax.de", "0.19");
            return parse(p, 0.19d);
        } else if ("US".equals(c)) {
            String p = System.getProperty("tax.us", "0.07");
            return parse(p, 0.07d);
        } else {
            return 0.0d;
        }
    }

    private static double parse(String s, double def) {
        try { return Double.parseDouble(s); } catch (Exception e) { return def; }
    }
}
