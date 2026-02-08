package com.acme.legacy.sca;

import java.util.Locale;
import java.util.function.Predicate;

/** Simple env/property-driven toggle with percentage canary. */
public final class EnvTogglePolicy implements TogglePolicy {
    private final boolean enabled;
    private final int percent;

    private EnvTogglePolicy(boolean enabled, int percent) {
        this.enabled = enabled;
        this.percent = Math.max(0, Math.min(100, percent));
    }
    public static EnvTogglePolicy current() {
        return new EnvTogglePolicy(
                readBool("order.new.enabled","ORDER_NEW_ENABLED",false),
                readInt ("order.new.percent","ORDER_NEW_PERCENT",0)
        );
    }
    @Override public boolean newEnabled() { return enabled; }
    @Override public Predicate<String> routePredicate() {
        return id -> (Math.abs((id == null ? 0 : id.hashCode())) % 100) < percent;
    }
    private static boolean readBool(String p,String e,boolean d){
        String v=System.getProperty(p); if(v==null)v=System.getenv(e);
        return v==null?d:v.toLowerCase(Locale.ROOT).matches("true|1|yes|on");
    }
    private static int readInt(String p,String e,int d){
        String v=System.getProperty(p); if(v==null)v=System.getenv(e);
        try{return v==null?d:Integer.parseInt(v);}catch(Exception ex){return d;}
    }
}
