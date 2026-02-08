package com.acme.legacy

import spock.lang.*

/**
 * Characterization spec that documents current legacy behavior.
 * Change these expectations only if business agrees to change behavior.
 */
class OrderServiceCharacterizationSpec extends Specification {

  def svc = new OrderService()

  @Unroll("gross(#orderId) == #expected")
  def "documents legacy gross behavior"() {
    expect:
    Math.abs(svc.sumGross(orderId) - expected) < 0.0001

    where:
    orderId    || expected
    "A-100"    || 119.00d
    "A-101"    || 238.00d
    "A-102"    || 107.00d   // US tax 7%
    "A-103"    || 119.00d   // includeTax=true -> treat stored as gross
    "A-ROUND"  || 119.99d   // rounding quirk preserved
  }
}
