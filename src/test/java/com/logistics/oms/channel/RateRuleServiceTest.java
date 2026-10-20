package com.logistics.oms.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.logistics.oms.cache.InMemoryCacheService;
import com.logistics.oms.module.channel.entity.RateRule;
import com.logistics.oms.module.channel.mapper.ChannelMapper;
import com.logistics.oms.module.channel.mapper.RateRuleMapper;
import com.logistics.oms.module.channel.service.RateRuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 价卡缓存测试：命中缓存不再查库；价卡变更淘汰缓存。
 */
class RateRuleServiceTest {

    private RateRuleMapper rateRuleMapper;
    private RateRuleService service;
    private RateRule rule;

    @BeforeEach
    void setUp() {
        rateRuleMapper = mock(RateRuleMapper.class);
        ChannelMapper channelMapper = mock(ChannelMapper.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        service = new RateRuleService(rateRuleMapper, channelMapper, new InMemoryCacheService(mapper));

        rule = new RateRule();
        rule.setId(1L);
        rule.setChannelId(1L);
        rule.setDestCountry("US");
        rule.setFirstWeight(new BigDecimal("0.5"));
        rule.setFirstPrice(new BigDecimal("18.00"));
        rule.setStepWeight(new BigDecimal("0.5"));
        rule.setStepPrice(new BigDecimal("8.00"));
        rule.setEffectiveDate(LocalDate.now().minusDays(1));
        rule.setExpiredDate(LocalDate.now().plusDays(1));
    }

    @Test
    void findEffective_cachesAfterFirstQuery() {
        when(rateRuleMapper.selectOne(any())).thenReturn(rule);

        assertNotNull(service.findEffective(1L, "US"));
        assertNotNull(service.findEffective(1L, "US"));
        assertNotNull(service.findEffective(1L, "US"));

        verify(rateRuleMapper, times(1)).selectOne(any());
    }

    @Test
    void update_evictsCacheAndQueriesDbAgain() {
        when(rateRuleMapper.selectOne(any())).thenReturn(rule);
        service.findEffective(1L, "US");
        verify(rateRuleMapper, times(1)).selectOne(any());

        when(rateRuleMapper.selectById(1L)).thenReturn(rule);
        when(rateRuleMapper.updateById(any())).thenReturn(1);
        service.update(1L, rule);

        service.findEffective(1L, "US");
        verify(rateRuleMapper, times(2)).selectOne(any());
    }
}
