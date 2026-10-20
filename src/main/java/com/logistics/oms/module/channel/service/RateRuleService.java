package com.logistics.oms.module.channel.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.logistics.oms.cache.CacheService;
import com.logistics.oms.common.BusinessException;
import com.logistics.oms.common.PageResult;
import com.logistics.oms.module.channel.entity.Channel;
import com.logistics.oms.module.channel.entity.RateRule;
import com.logistics.oms.module.channel.mapper.ChannelMapper;
import com.logistics.oms.module.channel.mapper.RateRuleMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

/**
 * 价卡服务：查价走缓存（key=rate:{channelId}:{country}），写价/改价即淘汰缓存。
 */
@Service
public class RateRuleService {

    private static final String CACHE_PREFIX = "rate:";

    private final RateRuleMapper rateRuleMapper;
    private final ChannelMapper channelMapper;
    private final CacheService cacheService;

    public RateRuleService(RateRuleMapper rateRuleMapper, ChannelMapper channelMapper, CacheService cacheService) {
        this.rateRuleMapper = rateRuleMapper;
        this.channelMapper = channelMapper;
        this.cacheService = cacheService;
    }

    /** 查询生效价卡（缓存优先，Redis 可用时显著降低 DB 压力）。 */
    public RateRule findEffective(Long channelId, String destCountry) {
        String key = cacheKey(channelId, destCountry);
        RateRule cached = cacheService.get(key, RateRule.class);
        if (cached != null) {
            return cached;
        }
        LocalDate today = LocalDate.now();
        RateRule rule = rateRuleMapper.selectOne(new QueryWrapper<RateRule>()
                .eq("channel_id", channelId)
                .eq("dest_country", destCountry)
                .le("effective_date", today)
                .ge("expired_date", today)
                .last("limit 1"));
        if (rule != null) {
            cacheService.set(key, rule, 3600);
        }
        return rule;
    }

    public PageResult<RateRule> page(long current, long size, Long channelId, String destCountry) {
        QueryWrapper<RateRule> w = new QueryWrapper<>();
        if (channelId != null) {
            w.eq("channel_id", channelId);
        }
        if (StringUtils.hasText(destCountry)) {
            w.eq("dest_country", destCountry.toUpperCase());
        }
        w.orderByAsc("channel_id").orderByAsc("dest_country");
        return PageResult.of(rateRuleMapper.selectPage(new Page<>(current, size), w));
    }

    public List<RateRule> listByChannel(Long channelId) {
        return rateRuleMapper.selectList(new QueryWrapper<RateRule>()
                .eq("channel_id", channelId)
                .orderByAsc("dest_country"));
    }

    public RateRule create(RateRule rule) {
        validateChannel(rule.getChannelId());
        if (rule.getEffectiveDate() == null || rule.getExpiredDate() == null) {
            throw BusinessException.param("生效/失效日期不能为空");
        }
        if (rateRuleMapper.selectCount(new QueryWrapper<RateRule>()
                .eq("channel_id", rule.getChannelId())
                .eq("dest_country", rule.getDestCountry())) > 0) {
            throw BusinessException.param("该渠道该目的国价卡已存在，请直接修改");
        }
        rule.setDestCountry(rule.getDestCountry().toUpperCase());
        rateRuleMapper.insert(rule);
        return rule;
    }

    public RateRule update(Long id, RateRule req) {
        RateRule rule = rateRuleMapper.selectById(id);
        if (rule == null) {
            throw BusinessException.notFound("价卡不存在");
        }
        rule.setFirstWeight(req.getFirstWeight());
        rule.setFirstPrice(req.getFirstPrice());
        rule.setStepWeight(req.getStepWeight());
        rule.setStepPrice(req.getStepPrice());
        rule.setFuelRate(req.getFuelRate());
        rule.setRegFee(req.getRegFee());
        rule.setCurrency(req.getCurrency());
        rule.setEffectiveDate(req.getEffectiveDate());
        rule.setExpiredDate(req.getExpiredDate());
        rateRuleMapper.updateById(rule);
        cacheService.delete(cacheKey(rule.getChannelId(), rule.getDestCountry()));
        return rule;
    }

    public void delete(Long id) {
        RateRule rule = rateRuleMapper.selectById(id);
        if (rule == null) {
            throw BusinessException.notFound("价卡不存在");
        }
        rateRuleMapper.deleteById(id);
        cacheService.delete(cacheKey(rule.getChannelId(), rule.getDestCountry()));
    }

    private void validateChannel(Long channelId) {
        Channel channel = channelMapper.selectById(channelId);
        if (channel == null) {
            throw BusinessException.param("渠道不存在");
        }
    }

    private String cacheKey(Long channelId, String destCountry) {
        return CACHE_PREFIX + channelId + ":" + (destCountry == null ? "" : destCountry.toUpperCase());
    }
}
