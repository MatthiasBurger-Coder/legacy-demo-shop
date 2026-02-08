package com.acme.legacy;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Legacy currency utilities with naive parsing/formatting. */
public class CurrencyUtil {

    public static double parseEuro(String s) {
        if (s == null) return 0d;
        s = s.replace("€", "").trim().replace(".", "").replace(",", ".");
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0d;
        }
    }

    public static String formatEuro(double v) {
        DecimalFormat df = new DecimalFormat("#,##0.00 "); // euro sign via locale symbols
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.GERMANY);
        sym.setCurrencySymbol("€");
        sym.setDecimalSeparator(',');
        sym.setGroupingSeparator('.');
        df.setDecimalFormatSymbols(sym);
        return df.format(v).replace(" ", " €");
    }
}
