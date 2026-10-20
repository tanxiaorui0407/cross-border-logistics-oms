package com.logistics.oms.order;

import com.logistics.oms.module.order.core.OrderStateMachine;
import com.logistics.oms.module.order.enums.OrderStatus;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 状态机合法性测试：合法流转放行、非法流转拦截、角色权限、终态无出口。
 */
class OrderStateMachineTest {

    @Test
    void legalTransitions() {
        assertTrue(OrderStateMachine.canTransition(OrderStatus.DRAFT, OrderStatus.SUBMITTED));
        assertTrue(OrderStateMachine.canTransition(OrderStatus.DRAFT, OrderStatus.CANCELED));
        assertTrue(OrderStateMachine.canTransition(OrderStatus.SUBMITTED, OrderStatus.PICKED_UP));
        assertTrue(OrderStateMachine.canTransition(OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT));
        assertTrue(OrderStateMachine.canTransition(OrderStatus.IN_TRANSIT, OrderStatus.CUSTOMS));
        assertTrue(OrderStateMachine.canTransition(OrderStatus.CUSTOMS, OrderStatus.DELIVERING));
        assertTrue(OrderStateMachine.canTransition(OrderStatus.DELIVERING, OrderStatus.SIGNED));
        assertTrue(OrderStateMachine.canTransition(OrderStatus.EXCEPTION, OrderStatus.IN_TRANSIT));
    }

    @Test
    void illegalTransitionsRejected() {
        assertFalse(OrderStateMachine.canTransition(OrderStatus.DRAFT, OrderStatus.SIGNED));
        assertFalse(OrderStateMachine.canTransition(OrderStatus.DRAFT, OrderStatus.IN_TRANSIT));
        assertFalse(OrderStateMachine.canTransition(OrderStatus.SUBMITTED, OrderStatus.SIGNED));
        assertFalse(OrderStateMachine.canTransition(OrderStatus.IN_TRANSIT, OrderStatus.SUBMITTED));
        assertFalse(OrderStateMachine.canTransition(OrderStatus.SIGNED, OrderStatus.IN_TRANSIT));
    }

    @Test
    void terminalStatesHaveNoOutgoing() {
        for (OrderStatus s : OrderStatus.values()) {
            if (s.isTerminal()) {
                assertTrue(OrderStateMachine.allowedTargets(s).isEmpty(), s + " 应为终态");
            }
        }
    }

    @Test
    void rolePermission() {
        // 客户可提交自己的草稿订单
        assertTrue(OrderStateMachine.roleAllowed(OrderStatus.DRAFT, OrderStatus.SUBMITTED, "CUSTOMER"));
        // 客户不可越权揽收
        assertFalse(OrderStateMachine.roleAllowed(OrderStatus.SUBMITTED, OrderStatus.PICKED_UP, "CUSTOMER"));
        // 操作员可揽收
        assertTrue(OrderStateMachine.roleAllowed(OrderStatus.SUBMITTED, OrderStatus.PICKED_UP, "OPS"));
        // 管理员拥有全部流转权限
        assertTrue(OrderStateMachine.roleAllowed(OrderStatus.DRAFT, OrderStatus.SUBMITTED, "ADMIN"));
        assertTrue(OrderStateMachine.roleAllowed(OrderStatus.DELIVERING, OrderStatus.SIGNED, "ADMIN"));
    }

    @Test
    void allowedTargets() {
        Set<OrderStatus> targets = OrderStateMachine.allowedTargets(OrderStatus.IN_TRANSIT);
        assertTrue(targets.contains(OrderStatus.CUSTOMS));
        assertTrue(targets.contains(OrderStatus.EXCEPTION));
    }
}
