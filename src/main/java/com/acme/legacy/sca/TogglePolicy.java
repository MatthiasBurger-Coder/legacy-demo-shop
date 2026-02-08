package com.acme.legacy.sca;

import java.util.function.Predicate;

/** C - Context/Policy defining when to route to the new path. */
public interface TogglePolicy {
    boolean newEnabled();
    Predicate<String> routePredicate(); // e.g., by id hash / tenant / allow-list
}
