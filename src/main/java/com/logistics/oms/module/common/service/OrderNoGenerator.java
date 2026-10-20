package com.logistics.oms.module.common.service;

import com.logistics.oms.common.BusinessException;
import com.logistics.oms.module.common.mapper.SysSeqMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 订单号发号器：SO + yyyyMMdd + 6 位日序号。
 * 基于 sys_seq 表 UPDATE 行锁递增（事务内 read-your-writes），
 * 生产环境可替换为 Redis INCR，这里做成可解释的数据库发号方案。
 */
@Service
public class OrderNoGenerator {

    public static final String SEQ_NAME = "order_no";

    private final SysSeqMapper sysSeqMapper;

    public OrderNoGenerator(SysSeqMapper sysSeqMapper) {
        this.sysSeqMapper = sysSeqMapper;
    }

    @Transactional
    public String nextOrderNo() {
        if (sysSeqMapper.increment(SEQ_NAME) == 0) {
            throw BusinessException.param("发号器未初始化，请联系管理员");
        }
        Long value = sysSeqMapper.selectValue(SEQ_NAME);
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return "SO" + date + String.format("%06d", value);
    }
}
