package com.logistics.oms.module.order.core;

import com.logistics.oms.module.order.enums.OrderStatus;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * 订单状态机（v2.0 集中式状态流转表）。
 *
 * <p>演进故事（面试讲点）：
 * v1.0 用 if/else 散落在业务代码里，新增状态要改 N 处；
 * 重构为「状态 → 允许的目标状态 → 允许触发角色」的集中 Map，
 * 非法流转在入口处一次性拦截，新增状态只需改一张表。</p>
 *
 * <p>并发安全由 OrderService 的乐观锁 UPDATE 保证（WHERE status=? AND version=?）。</p>
 */
public final class OrderStateMachine {

    /** from -> (to -> 允许触发的角色集合)。ADMIN 拥有全部流转权限。 */
    private static final Map<OrderStatus, Map<OrderStatus, Set<String>>> TRANSITIONS;

    static {
        Map<OrderStatus, Map<OrderStatus, Set<String>>> map = new EnumMap<>(OrderStatus.class);
        map.put(OrderStatus.DRAFT, Map.of(
                OrderStatus.SUBMITTED, Set.of("CUSTOMER"),
                OrderStatus.CANCELED, Set.of("CUSTOMER")));
        map.put(OrderStatus.SUBMITTED, Map.of(
                OrderStatus.PICKED_UP, Set.of("OPS"),
                OrderStatus.CANCELED, Set.of("OPS")));
        map.put(OrderStatus.PICKED_UP, Map.of(
                OrderStatus.IN_TRANSIT, Set.of("OPS")));
        map.put(OrderStatus.IN_TRANSIT, Map.of(
                OrderStatus.CUSTOMS, Set.of("OPS"),
                OrderStatus.EXCEPTION, Set.of("OPS")));
        map.put(OrderStatus.CUSTOMS, Map.of(
                OrderStatus.DELIVERING, Set.of("OPS"),
                OrderStatus.EXCEPTION, Set.of("OPS")));
        map.put(OrderStatus.DELIVERING, Map.of(
                OrderStatus.SIGNED, Set.of("OPS"),
                OrderStatus.REJECTED, Set.of("OPS")));
        map.put(OrderStatus.EXCEPTION, Map.of(
                OrderStatus.IN_TRANSIT, Set.of("OPS")));
        TRANSITIONS = Collections.unmodifiableMap(map);
    }

    /** 状态中文描述（系统轨迹事件文案）。 */
    public static final Map<OrderStatus, String> STATUS_DESC = Map.of(
            OrderStatus.DRAFT, "订单已创建",
            OrderStatus.SUBMITTED, "订单已提交",
            OrderStatus.PICKED_UP, "包裹已揽收",
            OrderStatus.IN_TRANSIT, "运输中",
            OrderStatus.CUSTOMS, "清关中",
            OrderStatus.DELIVERING, "派送中",
            OrderStatus.SIGNED, "已签收",
            OrderStatus.EXCEPTION, "异常处理中",
            OrderStatus.CANCELED, "订单已取消",
            OrderStatus.REJECTED, "已拒收");

    private OrderStateMachine() {
    }

    public static boolean canTransition(OrderStatus from, OrderStatus to) {
        Map<OrderStatus, Set<String>> targets = TRANSITIONS.get(from);
        return targets != null && targets.containsKey(to);
    }

    public static boolean roleAllowed(OrderStatus from, OrderStatus to, String role) {
        Map<OrderStatus, Set<String>> targets = TRANSITIONS.get(from);
        if (targets == null || !targets.containsKey(to)) {
            return false;
        }
        Set<String> roles = targets.get(to);
        return "ADMIN".equals(role) || roles.contains(role);
    }

    /** 当前状态允许流转到的目标状态（用于前端渲染可点按钮）。 */
    public static Set<OrderStatus> allowedTargets(OrderStatus from) {
        Map<OrderStatus, Set<String>> targets = TRANSITIONS.get(from);
        return targets == null ? Set.of() : targets.keySet();
    }

    public static String desc(OrderStatus status) {
        return STATUS_DESC.getOrDefault(status, status.name());
    }
}
