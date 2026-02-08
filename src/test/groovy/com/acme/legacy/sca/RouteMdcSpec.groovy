package com.acme.legacy.sca

import com.acme.legacy.OrderApi
import spock.lang.*
import org.slf4j.MDC
import java.util.function.Predicate

class RouteMdcSpec extends Specification {

    def "route=new when modern path is chosen"() {
        given:
        OrderApi legacy = { id -> 119.00d } as OrderApi
        OrderApi modern = { id ->
            assert MDC.get("route") == "new"
            200.00d
        } as OrderApi
        TogglePolicy on = Stub(TogglePolicy) {
            newEnabled() >> true
            routePredicate() >> ({ s -> true } as Predicate<String>)
        }

        expect:
        new SwitchingOrderApi(legacy, modern, on).sumGross("X") == 200.00d
    }

    def "route=legacy when modern is not selected"() {
        given:
        OrderApi legacy = { id ->
            assert MDC.get("route") == "legacy"
            119.00d
        } as OrderApi
        OrderApi modern = { id -> throw new AssertionError("should not be called") } as OrderApi
        TogglePolicy off = Stub(TogglePolicy) {
            newEnabled() >> false
            routePredicate() >> ({ s -> true } as Predicate<String>)
        }

        expect:
        new SwitchingOrderApi(legacy, modern, off).sumGross("Y") == 119.00d
    }

    def "route=fallback when modern throws"() {
        given:
        OrderApi legacy = { id ->
            assert MDC.get("route") == "fallback"
            119.00d
        } as OrderApi
        OrderApi boom = { id -> throw new RuntimeException("boom") } as OrderApi
        TogglePolicy on = Stub(TogglePolicy) {
            newEnabled() >> true
            routePredicate() >> ({ s -> true } as Predicate<String>)
        }

        expect:
        new SwitchingOrderApi(legacy, boom, on).sumGross("Z") == 119.00d
    }

    def "corrId + feature set; legacy route"() {
        given:
        OrderApi legacy = { id ->
            assert MDC.get("feature") == "order.sumGross"
            assert MDC.get("corrId")  != null
            assert MDC.get("route")   == "legacy"
            119.00d
        } as OrderApi
        TogglePolicy off = Stub(TogglePolicy) {
            newEnabled() >> false
            routePredicate() >> ({ s -> true } as Predicate<String>)
        }

        expect:
        new SwitchingOrderApi(legacy, { 0d } as OrderApi, off).sumGross("A-100") == 119.00d
    }
}

