package com.acme.logging;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface Traceable {
    String feature() default "";     // z.B. "order.sumGross"
    boolean logArgs() default false; // nur im DEBUG Modus sinnvoll
}
