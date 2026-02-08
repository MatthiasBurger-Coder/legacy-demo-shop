package com.acme.legacy;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Legacy repository reading a CSV from the classpath into an in-memory map.
 * Singleton with static access to simulate hard coupling.
 */
public class OrderRepository {
    private static OrderRepository INSTANCE;
    private final Map<String, Order> cache = new HashMap<>();

    public static OrderRepository getInstance() {
        if (INSTANCE == null) INSTANCE = new OrderRepository();
        return INSTANCE;
    }

    private OrderRepository() { load(); }

    private void load() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("orders.csv")) {
            if (in == null) return;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty() || line.startsWith("#")) continue;
                    String[] parts = line.split(";");
                    if (parts.length < 5) continue;
                    Order o = new Order();
                    o.id = parts[0];
                    o.customerId = parts[1];
                    o.country = parts[2];
                    o.netTotal = parts[3]; // e.g., "100,00"
                    o.includeTax = "1".equals(parts[4]);
                    cache.put(o.id, o);
                }
            }
        } catch (Exception e) {
            // swallow all (legacy style)
        }
    }

    public Order find(String id) {
        return cache.get(id);
    }
}
