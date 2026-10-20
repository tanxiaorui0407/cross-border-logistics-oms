package com.logistics.oms.module.order.enums;

/**
 * 订单状态（10 种），流转规则见 OrderStateMachine。
 */
public enum OrderStatus {

    DRAFT,
    SUBMITTED,
    PICKED_UP,
    IN_TRANSIT,
    CUSTOMS,
    DELIVERING,
    SIGNED,
    EXCEPTION,
    CANCELED,
    REJECTED;

    public boolean isTerminal() {
        return this == SIGNED || this == CANCELED || this == REJECTED;
    }
}
