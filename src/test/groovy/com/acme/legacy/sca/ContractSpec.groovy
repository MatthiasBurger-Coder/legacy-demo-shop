package com.acme.legacy.sca
import com.acme.legacy.OrderLegacy
import spock.lang.*
import java.math.BigDecimal

class ContractSpec extends Specification {

    @Unroll("id=#id -> legacy and new adapters agree on gross")
    def "legacy vs new adapter contract parity"() {
        given:
        def legacy = new LegacyOrderAdapter(new OrderLegacy(){
            @Override double sumGross(String orderId) {
                switch(orderId){
                    case "A-100": return 119.00d    // DE 19%
                    case "A-101": return 238.00d    // DE 19%
                    case "A-102": return 107.00d    // US 7%
                    case "A-103": return 119.00d    // includeTax true im Legacy
                    default: return 0d
                }
            }
        })

        // Demo-Netto-Daten (wie zuvor)
        def repo = { String oid ->
            switch(oid){
                case "A-100": return new BigDecimal("100.00")
                case "A-101": return new BigDecimal("200.00")
                case "A-102": return new BigDecimal("100.00")
                case "A-103": return new BigDecimal("100.00")
                default: return BigDecimal.ZERO
            }
        } as NewOrderAdapter.NetRepo

        // Steuer-Policy pro Order (hier simple ID-basierte Demo)
        def tax = { String oid ->
            switch(oid){
                case "A-102": return new BigDecimal("1.07") // US
                default:      return new BigDecimal("1.19") // DE
            }
        } as NewOrderAdapter.TaxPolicy

        def newAdapter = new NewOrderAdapter(repo, tax)

        expect:
        Math.abs(legacy.sumGross(id) - newAdapter.sumGross(id)) < 0.0001

        where:
        id << ["A-100","A-101","A-102","A-103"]
    }
}
