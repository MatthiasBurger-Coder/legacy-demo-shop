package com.acme.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Cross-cutting tracing aspect driven by @Traceable(feature="...").
 * - Generates corrId if absent (top-level only).
 * - Sets MDC("feature") to the annotation's feature; restores it afterwards.
 * - Logs entry/exit with duration.
 * - Cleans up corrId/route only at top-level to avoid MDC leaks.
 */
@Aspect
public class TracingAspect {

    private static final Logger log = LoggerFactory.getLogger(TracingAspect.class);

    // Match any method execution where either the method or the enclosing class is annotated.
    @Around("execution(* *(..)) && (@annotation(com.acme.logging.Traceable) || @within(com.acme.logging.Traceable))")
    public Object aroundTraceable(ProceedingJoinPoint pjp) throws Throwable {
        Traceable trace = resolveTraceable(pjp);
        // If neither method nor class has the annotation, just proceed (defensive).
        if (trace == null) {
            return pjp.proceed();
        }

        // Top-level detection: create corrId only if it's missing
        boolean createdCorr = false;
        if (MDC.get("corrId") == null) {
            MDC.put("corrId", UUID.randomUUID().toString());
            createdCorr = true;
        }

        // Save and set feature
        String prevFeature = MDC.get("feature");
        MDC.put("feature", trace.feature());

        // Prepare names for logging
        String cls = pjp.getSignature().getDeclaringTypeName();
        String method = pjp.getSignature().getName();

        long t0 = System.nanoTime();
        log.info("→ {}.{}", cls, method); // route is not yet known here

        try {
            Object out = pjp.proceed();

            long tookMs = (System.nanoTime() - t0) / 1_000_000;
            log.info("← {}.{} ok took={}ms", cls, method, tookMs); // route present if set in method (and not cleared)

            return out;
        } catch (Throwable ex) {
            long tookMs = (System.nanoTime() - t0) / 1_000_000;
            log.warn("← {}.{} fail took={}ms ex={}", cls, method, tookMs, ex.toString());
            throw ex;
        } finally {
            // Restore/cleanup in correct order
            if (prevFeature == null) {
                MDC.remove("feature");
            } else {
                MDC.put("feature", prevFeature);
            }

            // Only top-level scope is allowed to clean up corrId and route.
            if (createdCorr) {
                MDC.remove("route");   // keep 'route' for exit log; cleanup happens here afterwards
                MDC.remove("corrId");
            }
        }
    }

    // Find @Traceable on method, else on class
    private Traceable resolveTraceable(ProceedingJoinPoint pjp) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method m = sig.getMethod();

        Traceable ann = m.getAnnotation(Traceable.class);
        if (ann != null) return ann;

        Class<?> targetCls = pjp.getTarget() != null ? pjp.getTarget().getClass() : sig.getDeclaringType();
        return targetCls.getAnnotation(Traceable.class);
    }
}
