package com.acme.legacy;

/** Legacy data holder with public fields and weak typing (intentional). */
public class Order {
    public String id;
    public String customerId;
    public String country;   // e.g., "DE" or "US"
    public String netTotal;  // localized decimal string, e.g., "100,00"
    public boolean includeTax;
}
