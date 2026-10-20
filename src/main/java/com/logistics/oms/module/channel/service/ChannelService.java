package com.logistics.oms.module.channel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.logistics.oms.common.BusinessException;
import com.logistics.oms.common.PageResult;
import com.logistics.oms.module.channel.entity.Channel;
import com.logistics.oms.module.channel.mapper.ChannelMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ChannelService {

    private final ChannelMapper channelMapper;

    public ChannelService(ChannelMapper channelMapper) {
        this.channelMapper = channelMapper;
    }

    public List<Channel> listActive() {
        return channelMapper.selectList(new LambdaQueryWrapper<Channel>()
                .eq(Channel::getStatus, 1)
                .orderByAsc(Channel::getId));
    }

    public PageResult<Channel> page(long current, long size, String keyword) {
        LambdaQueryWrapper<Channel> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            w.and(x -> x.like(Channel::getChannelName, keyword)
                    .or().like(Channel::getChannelCode, keyword));
        }
        w.orderByAsc(Channel::getId);
        return PageResult.of(channelMapper.selectPage(new Page<>(current, size), w));
    }

    public Channel create(Channel channel) {
        if (channelMapper.selectCount(new LambdaQueryWrapper<Channel>()
                .eq(Channel::getChannelCode, channel.getChannelCode())) > 0) {
            throw BusinessException.param("渠道编码已存在");
        }
        fillDefaults(channel);
        channelMapper.insert(channel);
        return channel;
    }

    public Channel update(Long id, Channel req) {
        Channel channel = channelMapper.selectById(id);
        if (channel == null) {
            throw BusinessException.notFound("渠道不存在");
        }
        channel.setChannelName(req.getChannelName());
        channel.setTransportType(req.getTransportType());
        channel.setCarrier(req.getCarrier());
        channel.setEtaDays(req.getEtaDays());
        channel.setVolDivisor(req.getVolDivisor());
        channel.setCurrency(req.getCurrency());
        channel.setStatus(req.getStatus());
        channelMapper.updateById(channel);
        return channel;
    }

    public void delete(Long id) {
        if (channelMapper.deleteById(id) == 0) {
            throw BusinessException.notFound("渠道不存在");
        }
    }

    public Map<Long, Channel> mapByIds(java.util.Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return channelMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Channel::getId, Function.identity(), (a, b) -> a));
    }

    private void fillDefaults(Channel channel) {
        if (channel.getEtaDays() == null) {
            channel.setEtaDays(7);
        }
        if (channel.getVolDivisor() == null) {
            channel.setVolDivisor(5000);
        }
        if (channel.getCurrency() == null) {
            channel.setCurrency("USD");
        }
        if (channel.getStatus() == null) {
            channel.setStatus(1);
        }
    }
}
