package com.logistics.oms.module.quote.service;

import com.logistics.oms.common.BusinessException;
import com.logistics.oms.module.channel.entity.Channel;
import com.logistics.oms.module.channel.entity.RateRule;
import com.logistics.oms.module.channel.service.ChannelService;
import com.logistics.oms.module.channel.service.RateRuleService;
import com.logistics.oms.module.quote.core.FreightCalculator;
import com.logistics.oms.module.quote.dto.QuoteItemVO;
import com.logistics.oms.module.quote.dto.QuoteReq;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 运费试算：多渠道比价，按价格升序返回。
 */
@Service
public class QuoteService {

    private final ChannelService channelService;
    private final RateRuleService rateRuleService;
    private final FreightCalculator freightCalculator;

    public QuoteService(ChannelService channelService, RateRuleService rateRuleService,
                        FreightCalculator freightCalculator) {
        this.channelService = channelService;
        this.rateRuleService = rateRuleService;
        this.freightCalculator = freightCalculator;
    }

    public List<QuoteItemVO> preview(QuoteReq req) {
        if (req.getActualWeight().compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.param("实重必须大于0");
        }
        List<QuoteItemVO> items = new ArrayList<>();
        for (Channel channel : channelService.listActive()) {
            RateRule rule = rateRuleService.findEffective(channel.getId(), req.getDestCountry());
            if (rule == null) {
                continue; // 该渠道无目的国价卡，不参与比价
            }
            FreightCalculator.CalcResult r = freightCalculator.calc(
                    channel, rule, req.getActualWeight(),
                    req.getLengthCm(), req.getWidthCm(), req.getHeightCm());
            QuoteItemVO vo = new QuoteItemVO();
            vo.setChannelId(channel.getId());
            vo.setChannelName(channel.getChannelName());
            vo.setTransportType(channel.getTransportType());
            vo.setEtaDays(channel.getEtaDays());
            vo.setVolWeight(r.volWeight());
            vo.setBillableWeight(r.billableWeight());
            vo.setFreight(r.freight());
            vo.setCurrency(channel.getCurrency());
            items.add(vo);
        }
        items.sort(Comparator.comparing(QuoteItemVO::getFreight));
        return items;
    }
}
