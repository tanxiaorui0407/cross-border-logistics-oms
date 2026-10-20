package com.logistics.oms.module.order.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.logistics.oms.common.BusinessException;
import com.logistics.oms.common.PageResult;
import com.logistics.oms.module.channel.entity.Channel;
import com.logistics.oms.module.channel.entity.RateRule;
import com.logistics.oms.module.channel.service.ChannelService;
import com.logistics.oms.module.channel.service.RateRuleService;
import com.logistics.oms.module.customer.entity.Customer;
import com.logistics.oms.module.customer.mapper.CustomerMapper;
import com.logistics.oms.module.order.core.OrderStateMachine;
import com.logistics.oms.module.order.dto.AddressReq;
import com.logistics.oms.module.order.dto.AddressVO;
import com.logistics.oms.module.order.dto.OrderCreateReq;
import com.logistics.oms.module.order.dto.OrderQuery;
import com.logistics.oms.module.order.dto.OrderVO;
import com.logistics.oms.module.order.dto.TrackEventReq;
import com.logistics.oms.module.order.dto.TrackEventVO;
import com.logistics.oms.module.order.dto.TransitionReq;
import com.logistics.oms.module.order.entity.OrderAddress;
import com.logistics.oms.module.order.entity.OrderMain;
import com.logistics.oms.module.order.entity.OrderTrackEvent;
import com.logistics.oms.module.order.enums.OrderStatus;
import com.logistics.oms.module.order.mapper.OrderAddressMapper;
import com.logistics.oms.module.order.mapper.OrderMainMapper;
import com.logistics.oms.module.common.service.OrderNoGenerator;
import com.logistics.oms.module.quote.core.FreightCalculator;
import com.logistics.oms.security.LoginUser;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单核心服务：
 * 1. 下单（幂等 token + DB 唯一索引兜底）
 * 2. 角色数据可见性（CUSTOMER 只能看到自己的订单）
 * 3. 状态流转（状态机校验 + 乐观锁并发控制）
 * 4. 轨迹录入、状态统计
 */
@Service
public class OrderService {

    private final OrderMainMapper orderMapper;
    private final OrderAddressMapper addressMapper;
    private final CustomerMapper customerMapper;
    private final ChannelService channelService;
    private final RateRuleService rateRuleService;
    private final FreightCalculator freightCalculator;
    private final TrackService trackService;
    private final OrderNoGenerator orderNoGenerator;

    public OrderService(OrderMainMapper orderMapper,
                        OrderAddressMapper addressMapper,
                        CustomerMapper customerMapper,
                        ChannelService channelService,
                        RateRuleService rateRuleService,
                        FreightCalculator freightCalculator,
                        TrackService trackService,
                        OrderNoGenerator orderNoGenerator) {
        this.orderMapper = orderMapper;
        this.addressMapper = addressMapper;
        this.customerMapper = customerMapper;
        this.channelService = channelService;
        this.rateRuleService = rateRuleService;
        this.freightCalculator = freightCalculator;
        this.trackService = trackService;
        this.orderNoGenerator = orderNoGenerator;
    }

    // ---------- 下单 ----------

    @Transactional
    public OrderVO create(OrderCreateReq req, LoginUser user, String idemToken) {
        Long customerId = resolveCustomerId(req.getCustomerId(), user);
        String token = StringUtils.hasText(idemToken) ? idemToken.trim() : null;

        // 幂等：token 已存在 → 直接返回已有订单
        if (token != null) {
            OrderMain exist = orderMapper.selectOne(new QueryWrapper<OrderMain>()
                    .eq("idem_token", token)
                    .last("limit 1"));
            if (exist != null) {
                return detail(exist.getId(), user);
            }
        }

        Channel channel = channelService.listActive().stream()
                .filter(c -> c.getId().equals(req.getChannelId()))
                .findFirst()
                .orElseThrow(() -> BusinessException.param("物流渠道不存在或已停用"));

        RateRule rule = rateRuleService.findEffective(channel.getId(), req.getDestCountry());
        if (rule == null) {
            throw BusinessException.param("渠道[" + channel.getChannelName() + "]暂无目的国["
                    + req.getDestCountry() + "]的价卡");
        }
        if (req.getActualWeight() == null || req.getActualWeight().compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.param("实重必须大于0");
        }

        FreightCalculator.CalcResult calc = freightCalculator.calc(
                channel, rule, req.getActualWeight(),
                req.getLengthCm(), req.getWidthCm(), req.getHeightCm());

        OrderMain order = new OrderMain();
        order.setOrderNo(orderNoGenerator.nextOrderNo());
        order.setCustomerId(customerId);
        order.setChannelId(channel.getId());
        order.setStatus(OrderStatus.DRAFT.name());
        order.setVersion(0);
        order.setIdemToken(token);
        order.setGoodsDesc(req.getGoodsDesc());
        order.setPieces(req.getPieces() == null ? 1 : req.getPieces());
        order.setActualWeight(req.getActualWeight());
        order.setLengthCm(req.getLengthCm());
        order.setWidthCm(req.getWidthCm());
        order.setHeightCm(req.getHeightCm());
        order.setVolWeight(calc.volWeight());
        order.setBillableWeight(calc.billableWeight());
        order.setFreight(calc.freight());
        order.setCurrency(channel.getCurrency() == null ? "USD" : channel.getCurrency());

        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            // 并发同 token 重复提交，唯一索引兜底，回查返回已有订单
            if (token != null) {
                OrderMain exist = orderMapper.selectOne(new QueryWrapper<OrderMain>()
                        .eq("idem_token", token)
                        .last("limit 1"));
                if (exist != null) {
                    return detail(exist.getId(), user);
                }
            }
            throw e;
        }

        insertAddress(order.getId(), "SENDER", req.getSender());
        insertAddress(order.getId(), "RECEIVER", req.getReceiver());
        trackService.addSystemEvent(order.getId(), OrderStatus.DRAFT.name(),
                OrderStateMachine.desc(OrderStatus.DRAFT), "SYSTEM");
        return detail(order.getId(), user);
    }

    // ---------- 分页查询 ----------

    public PageResult<OrderVO> page(OrderQuery q, LoginUser user) {
        QueryWrapper<OrderMain> w = new QueryWrapper<>();
        if ("CUSTOMER".equals(user.role())) {
            w.eq("customer_id", user.customerId());
        }
        if (StringUtils.hasText(q.getStatus())) {
            w.eq("status", q.getStatus());
        }
        if (StringUtils.hasText(q.getKeyword())) {
            w.and(x -> x.like("order_no", q.getKeyword())
                    .or().like("goods_desc", q.getKeyword()));
        }
        if (q.getCustomerId() != null && !"CUSTOMER".equals(user.role())) {
            w.eq("customer_id", q.getCustomerId());
        }
        if (q.getStartDate() != null) {
            w.ge("created_at", q.getStartDate().atStartOfDay());
        }
        if (q.getEndDate() != null) {
            w.lt("created_at", q.getEndDate().plusDays(1).atStartOfDay());
        }
        w.orderByDesc("created_at");

        Page<OrderMain> p = orderMapper.selectPage(new Page<>(q.getPage(), q.getSize()), w);
        Map<Long, String> customerNames = customerNames(p.getRecords());
        Map<Long, Channel> channelMap = channelService.mapByIds(
                p.getRecords().stream().map(OrderMain::getChannelId).collect(Collectors.toSet()));
        return PageResult.of(p, o -> toListVO(o, customerNames, channelMap));
    }

    // ---------- 详情 ----------

    public OrderVO detail(Long id, LoginUser user) {
        OrderMain order = getVisible(id, user);
        Map<Long, String> customerNames = customerNames(List.of(order));
        Map<Long, Channel> channelMap = channelService.mapByIds(Set.of(order.getChannelId()));
        OrderVO vo = toListVO(order, customerNames, channelMap);

        List<OrderAddress> addresses = addressMapper.selectList(
                new QueryWrapper<OrderAddress>().eq("order_id", id));
        for (OrderAddress a : addresses) {
            AddressVO av = new AddressVO();
            av.setAddrType(a.getAddrType());
            av.setName(a.getName());
            av.setPhone(a.getPhone());
            av.setCountry(a.getCountry());
            av.setCity(a.getCity());
            av.setStreet(a.getStreet());
            av.setZip(a.getZip());
            if ("SENDER".equals(a.getAddrType())) {
                vo.setSender(av);
            } else if ("RECEIVER".equals(a.getAddrType())) {
                vo.setReceiver(av);
            }
        }

        List<TrackEventVO> events = trackService.listByOrder(id).stream().map(e -> {
            TrackEventVO ev = new TrackEventVO();
            ev.setEventCode(e.getEventCode());
            ev.setEventDesc(e.getEventDesc());
            ev.setLocation(e.getLocation());
            ev.setEventTime(e.getEventTime());
            ev.setOperator(e.getOperator());
            return ev;
        }).collect(Collectors.toList());
        vo.setTrackEvents(events);

        OrderStatus from = OrderStatus.valueOf(order.getStatus());
        List<String> allowed = OrderStateMachine.allowedTargets(from).stream()
                .filter(to -> OrderStateMachine.roleAllowed(from, to, user.role()))
                .map(Enum::name)
                .collect(Collectors.toList());
        vo.setAllowedTransitions(allowed);
        return vo;
    }

    // ---------- 状态流转（状态机 + 乐观锁） ----------

    @Transactional
    public OrderVO transition(Long id, TransitionReq req, LoginUser user) {
        OrderMain order = getVisible(id, user);
        OrderStatus from = OrderStatus.valueOf(order.getStatus());
        OrderStatus to;
        try {
            to = OrderStatus.valueOf(req.getTargetStatus());
        } catch (IllegalArgumentException e) {
            throw BusinessException.param("非法目标状态");
        }

        if (!OrderStateMachine.canTransition(from, to)) {
            throw BusinessException.conflict("非法状态流转: " + from + " → " + to);
        }
        if (!OrderStateMachine.roleAllowed(from, to, user.role())) {
            throw BusinessException.forbidden("当前角色无权执行该流转");
        }

        // 乐观锁：WHERE status=? AND version=?，影响行数为 0 说明已被他人变更
        int rows = orderMapper.update(null, new UpdateWrapper<OrderMain>()
                .set("status", to.name())
                .set("version", order.getVersion() + 1)
                .eq("id", order.getId())
                .eq("status", from.name())
                .eq("version", order.getVersion()));
        if (rows == 0) {
            throw BusinessException.conflict("订单状态已被其他操作员变更，请刷新后重试");
        }

        trackService.addSystemEvent(order.getId(), to.name(),
                OrderStateMachine.desc(to), user.username());
        return detail(id, user);
    }

    // ---------- 手工轨迹（OPS） ----------

    public void addTrackEvent(Long id, TrackEventReq req, LoginUser user) {
        if (!"OPS".equals(user.role()) && !"ADMIN".equals(user.role())) {
            throw BusinessException.forbidden("仅操作员可录入轨迹");
        }
        getVisible(id, user);
        if (req.getEventDesc() == null || req.getEventDesc().isBlank()) {
            throw BusinessException.param("轨迹描述不能为空");
        }
        trackService.addManualEvent(id, req, user.username());
    }

    // ---------- 状态统计 ----------

    public Map<String, Long> stats(LoginUser user) {
        QueryWrapper<OrderMain> w = new QueryWrapper<>();
        w.select("status", "COUNT(*) AS cnt");
        if ("CUSTOMER".equals(user.role())) {
            w.eq("customer_id", user.customerId());
        }
        w.groupBy("status");
        Map<String, Long> result = new LinkedHashMap<>();
        long total = 0;
        for (Map<String, Object> row : orderMapper.selectMaps(w)) {
            String status = String.valueOf(row.get("status"));
            long cnt = ((Number) row.get("cnt")).longValue();
            result.put(status, cnt);
            total += cnt;
        }
        result.put("TOTAL", total);
        return result;
    }

    // ---------- 内部方法 ----------

    private OrderMain getVisible(Long id, LoginUser user) {
        QueryWrapper<OrderMain> w = new QueryWrapper<OrderMain>().eq("id", id);
        if ("CUSTOMER".equals(user.role())) {
            w.eq("customer_id", user.customerId());
        }
        OrderMain order = orderMapper.selectOne(w);
        if (order == null) {
            throw BusinessException.notFound("订单不存在或无权访问");
        }
        return order;
    }

    private Long resolveCustomerId(Long reqCustomerId, LoginUser user) {
        Long customerId;
        if ("CUSTOMER".equals(user.role())) {
            customerId = user.customerId();
            if (customerId == null) {
                throw BusinessException.forbidden("当前账号未关联客户");
            }
        } else {
            customerId = reqCustomerId;
            if (customerId == null) {
                throw BusinessException.param("请指定客户");
            }
        }
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null || customer.getStatus() == null || customer.getStatus() != 1) {
            throw BusinessException.param("客户不存在或已停用");
        }
        return customerId;
    }

    private void insertAddress(Long orderId, String type, AddressReq req) {
        OrderAddress addr = new OrderAddress();
        addr.setOrderId(orderId);
        addr.setAddrType(type);
        addr.setName(req.getName());
        addr.setPhone(req.getPhone());
        addr.setCountry(req.getCountry());
        addr.setCity(req.getCity());
        addr.setStreet(req.getStreet());
        addr.setZip(req.getZip());
        addressMapper.insert(addr);
    }

    private Map<Long, String> customerNames(List<OrderMain> orders) {
        Set<Long> ids = orders.stream().map(OrderMain::getCustomerId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return customerMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getCompanyName, (a, b) -> a));
    }

    private OrderVO toListVO(OrderMain o, Map<Long, String> customerNames, Map<Long, Channel> channelMap) {
        OrderVO vo = new OrderVO();
        vo.setId(o.getId());
        vo.setOrderNo(o.getOrderNo());
        vo.setCustomerId(o.getCustomerId());
        vo.setCustomerName(customerNames.get(o.getCustomerId()));
        vo.setChannelId(o.getChannelId());
        Channel channel = channelMap.get(o.getChannelId());
        if (channel != null) {
            vo.setChannelName(channel.getChannelName());
            vo.setTransportType(channel.getTransportType());
        }
        vo.setStatus(o.getStatus());
        vo.setStatusDesc(OrderStateMachine.desc(OrderStatus.valueOf(o.getStatus())));
        vo.setGoodsDesc(o.getGoodsDesc());
        vo.setPieces(o.getPieces());
        vo.setActualWeight(o.getActualWeight());
        vo.setVolWeight(o.getVolWeight());
        vo.setBillableWeight(o.getBillableWeight());
        vo.setFreight(o.getFreight());
        vo.setCurrency(o.getCurrency());
        vo.setCreatedAt(o.getCreatedAt());
        return vo;
    }
}
