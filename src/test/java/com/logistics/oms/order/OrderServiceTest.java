package com.logistics.oms.order;

import com.logistics.oms.common.BusinessException;
import com.logistics.oms.module.channel.entity.Channel;
import com.logistics.oms.module.channel.entity.RateRule;
import com.logistics.oms.module.channel.service.ChannelService;
import com.logistics.oms.module.channel.service.RateRuleService;
import com.logistics.oms.module.common.service.OrderNoGenerator;
import com.logistics.oms.module.customer.entity.Customer;
import com.logistics.oms.module.customer.mapper.CustomerMapper;
import com.logistics.oms.module.order.dto.AddressReq;
import com.logistics.oms.module.order.dto.OrderCreateReq;
import com.logistics.oms.module.order.dto.OrderVO;
import com.logistics.oms.module.order.dto.TransitionReq;
import com.logistics.oms.module.order.entity.OrderMain;
import com.logistics.oms.module.order.enums.OrderStatus;
import com.logistics.oms.module.order.mapper.OrderAddressMapper;
import com.logistics.oms.module.order.mapper.OrderMainMapper;
import com.logistics.oms.module.order.service.OrderService;
import com.logistics.oms.module.order.service.TrackService;
import com.logistics.oms.module.quote.core.FreightCalculator;
import com.logistics.oms.security.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 订单服务测试：下单幂等、状态流转（状态机+乐观锁）、角色可见性。
 */
class OrderServiceTest {

    private OrderMainMapper orderMapper;
    private OrderAddressMapper addressMapper;
    private CustomerMapper customerMapper;
    private ChannelService channelService;
    private RateRuleService rateRuleService;
    private FreightCalculator freightCalculator;
    private TrackService trackService;
    private OrderNoGenerator orderNoGenerator;
    private OrderService orderService;

    private Channel channel;
    private RateRule rule;
    private Customer customer;
    private LoginUser ops = new LoginUser(10L, "ops", "OPS", null);
    private LoginUser alice = new LoginUser(11L, "alice", "CUSTOMER", 1L);

    @BeforeEach
    void setUp() {
        orderMapper = mock(OrderMainMapper.class);
        addressMapper = mock(OrderAddressMapper.class);
        customerMapper = mock(CustomerMapper.class);
        channelService = mock(ChannelService.class);
        rateRuleService = mock(RateRuleService.class);
        freightCalculator = mock(FreightCalculator.class);
        trackService = mock(TrackService.class);
        orderNoGenerator = mock(OrderNoGenerator.class);
        orderService = new OrderService(orderMapper, addressMapper, customerMapper,
                channelService, rateRuleService, freightCalculator, trackService, orderNoGenerator);

        channel = new Channel();
        channel.setId(1L);
        channel.setChannelName("国际快递");
        channel.setTransportType("EXPRESS");
        channel.setCurrency("USD");
        rule = new RateRule();
        rule.setId(1L);
        rule.setChannelId(1L);
        rule.setDestCountry("US");
        customer = new Customer();
        customer.setId(1L);
        customer.setCompanyName("深圳飞鱼贸易有限公司");
        customer.setStatus(1);

        when(channelService.listActive()).thenReturn(List.of(channel));
        when(channelService.mapByIds(anySet())).thenReturn(Map.of(1L, channel));
        when(rateRuleService.findEffective(1L, "US")).thenReturn(rule);
        when(freightCalculator.calc(any(), any(), any(), any(), any(), any()))
                .thenReturn(new FreightCalculator.CalcResult(new BigDecimal("0.600"),
                        new BigDecimal("0.600"), new BigDecimal("27.30")));
        when(orderNoGenerator.nextOrderNo()).thenReturn("SO202610200001");
        when(customerMapper.selectById(1L)).thenReturn(customer);
        when(customerMapper.selectBatchIds(anySet())).thenReturn(List.of());
        when(addressMapper.selectList(any())).thenReturn(List.of());
        when(trackService.listByOrder(anyLong())).thenReturn(List.of());
    }

    private OrderCreateReq createReq() {
        OrderCreateReq req = new OrderCreateReq();
        req.setChannelId(1L);
        req.setCustomerId(1L);
        req.setGoodsDesc("蓝牙耳机");
        req.setPieces(1);
        req.setActualWeight(new BigDecimal("0.500"));
        req.setDestCountry("US");
        AddressReq sender = new AddressReq();
        sender.setName("Alice");
        sender.setCountry("CN");
        req.setSender(sender);
        AddressReq receiver = new AddressReq();
        receiver.setName("John");
        receiver.setCountry("US");
        req.setReceiver(receiver);
        return req;
    }

    @Test
    void createOrder_computesFreightAndInserts() {
        doAnswer(inv -> {
            OrderMain o = inv.getArgument(0);
            o.setId(1L);
            return 1;
        }).when(orderMapper).insert(any());
        // create() 中 token 为空 → 跳过 token 查询，第一次 selectOne 即 detail 的 getVisible
        when(orderMapper.selectOne(any())).thenReturn(orderOf(1L, OrderStatus.DRAFT));

        OrderVO vo = orderService.create(createReq(), ops, null);

        assertNotNull(vo);
        verify(orderMapper, times(1)).insert(any());
        verify(trackService).addSystemEvent(anyLong(), any(), any(), any());
    }

    @Test
    void createOrder_idempotencyTokenReturnsSameOrder() {
        // 第一次创建：token 查询(call1) → null；插入；detail getVisible(call2)
        // 第二次创建：token 查询(call3) → 已有订单；detail getVisible(call4)
        when(orderMapper.selectOne(any())).thenReturn(
                null,
                orderOf(1L, OrderStatus.DRAFT),
                orderOf(1L, OrderStatus.DRAFT),
                orderOf(1L, OrderStatus.DRAFT));
        doAnswer(inv -> {
            OrderMain o = inv.getArgument(0);
            o.setId(1L);
            return 1;
        }).when(orderMapper).insert(any());

        orderService.create(createReq(), ops, "TOKEN-1");
        OrderVO vo = orderService.create(createReq(), ops, "TOKEN-1");

        assertNotNull(vo);
        verify(orderMapper, times(1)).insert(any());
    }

    @Test
    void transition_success() {
        OrderMain order = orderOf(1L, OrderStatus.SUBMITTED);
        when(orderMapper.selectOne(any())).thenReturn(order, order);
        when(orderMapper.update(isNull(), any())).thenReturn(1);

        OrderVO vo = orderService.transition(1L, trans("PICKED_UP"), ops);

        assertNotNull(vo);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(trackService).addSystemEvent(anyLong(), captor.capture(), any(), any());
        assertEquals("PICKED_UP", captor.getValue());
    }

    @Test
    void transition_optimisticLockConflict() {
        OrderMain order = orderOf(1L, OrderStatus.SUBMITTED);
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(orderMapper.update(isNull(), any())).thenReturn(0); // 乐观锁冲突

        BusinessException e = assertThrows(BusinessException.class,
                () -> orderService.transition(1L, trans("PICKED_UP"), ops));
        assertEquals(409, e.getCode());
        assertEquals("订单状态已被其他操作员变更，请刷新后重试", e.getMessage());
    }

    @Test
    void transition_illegalStateMachineRejected() {
        OrderMain order = orderOf(1L, OrderStatus.DRAFT);
        when(orderMapper.selectOne(any())).thenReturn(order);

        BusinessException e = assertThrows(BusinessException.class,
                () -> orderService.transition(1L, trans("SIGNED"), ops));
        assertEquals(409, e.getCode());
    }

    @Test
    void transition_roleNotAllowed() {
        // CUSTOMER 无权把订单流转为 PICKED_UP
        OrderMain order = orderOf(1L, OrderStatus.SUBMITTED);
        when(orderMapper.selectOne(any())).thenReturn(order);

        BusinessException e = assertThrows(BusinessException.class,
                () -> orderService.transition(1L, trans("PICKED_UP"), alice));
        assertEquals(403, e.getCode());
    }

    @Test
    void detail_customerCanOnlySeeOwnOrder() {
        // CUSTOMER 访问他人订单 → selectOne 按 customerId 过滤返回 null → 404
        when(orderMapper.selectOne(any())).thenReturn(null);
        BusinessException e = assertThrows(BusinessException.class,
                () -> orderService.detail(999L, alice));
        assertEquals(404, e.getCode());
    }

    private OrderMain orderOf(Long id, OrderStatus status) {
        OrderMain o = new OrderMain();
        o.setId(id);
        o.setOrderNo("SO20261020000" + id);
        o.setCustomerId(1L);
        o.setChannelId(1L);
        o.setStatus(status.name());
        o.setVersion(0);
        o.setFreight(new BigDecimal("27.30"));
        return o;
    }

    private TransitionReq trans(String target) {
        TransitionReq req = new TransitionReq();
        req.setTargetStatus(target);
        return req;
    }
}
