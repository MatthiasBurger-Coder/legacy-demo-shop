package com.acme.legacy.sca

import com.acme.legacy.OrderApi
import spock.lang.*

import java.util.function.Predicate

class SwitchingOrderApiSpec extends Specification {

    def "routes to modern when enabled and predicate true; else legacy"() {
        given:
        OrderApi legacy = { id -> 119.00d } as OrderApi
        OrderApi modern = { id -> 200.00d } as OrderApi
        TogglePolicy on = Stub(TogglePolicy) {
            newEnabled() >> true
            routePredicate() >> ({ s -> true } as Predicate<String>)
        }

        TogglePolicy off = Stub(TogglePolicy) {
            newEnabled() >> false
            routePredicate() >> ({ s -> true } as Predicate<String>)
        }

        expect:
        new SwitchingOrderApi(legacy, modern, on ).sumGross("X") == 200.00d
        new SwitchingOrderApi(legacy, modern, off).sumGross("X") == 119.00d
    }

    def "falls back to legacy when modern throws"() {
        given:
        OrderApi legacy = { id -> 119.00d } as OrderApi
        OrderApi boom   = { id -> throw new RuntimeException("boom") } as OrderApi
        def policy = Stub(TogglePolicy) {
            newEnabled() >> true
            routePredicate() >> ({ __ -> true } as Predicate<String>)
        }

        expect:
        new SwitchingOrderApi(legacy, boom, policy).sumGross("A") == 119.00d
    }

}
