package com.logistics.oms.module.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.logistics.oms.common.BusinessException;
import com.logistics.oms.module.order.dto.TrackEventReq;
import com.logistics.oms.module.order.entity.OrderTrackEvent;
import com.logistics.oms.module.order.mapper.OrderTrackEventMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 轨迹服务：状态机系统事件 + 操作员手工录入。
 */
@Service
public class TrackService {

    private final OrderTrackEventMapper trackEventMapper;

    public TrackService(OrderTrackEventMapper trackEventMapper) {
        this.trackEventMapper = trackEventMapper;
    }

    /** 状态变更自动落一条系统轨迹。 */
    public void addSystemEvent(Long orderId, String eventCode, String eventDesc, String operator) {
        add(orderId, eventCode, eventDesc, null, LocalDateTime.now(),
                operator == null || operator.isBlank() ? "SYSTEM" : operator);
    }

    public void addManualEvent(Long orderId, TrackEventReq req, String operator) {
        add(orderId,
                req.getEventCode() == null || req.getEventCode().isBlank() ? "CUSTOM" : req.getEventCode(),
                req.getEventDesc(),
                req.getLocation(),
                req.getEventTime() == null ? LocalDateTime.now() : req.getEventTime(),
                operator);
    }

    public List<OrderTrackEvent> listByOrder(Long orderId) {
        return trackEventMapper.selectList(new LambdaQueryWrapper<OrderTrackEvent>()
                .eq(OrderTrackEvent::getOrderId, orderId)
                .orderByAsc(OrderTrackEvent::getEventTime)
                .orderByAsc(OrderTrackEvent::getId));
    }

    private void add(Long orderId, String eventCode, String eventDesc, String location,
                     LocalDateTime eventTime, String operator) {
        OrderTrackEvent event = new OrderTrackEvent();
        event.setOrderId(orderId);
        event.setEventCode(eventCode);
        event.setEventDesc(eventDesc);
        event.setLocation(location);
        event.setEventTime(eventTime);
        event.setOperator(operator);
        trackEventMapper.insert(event);
    }
}
