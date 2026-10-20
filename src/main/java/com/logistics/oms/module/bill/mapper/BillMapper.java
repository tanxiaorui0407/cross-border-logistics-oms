package com.logistics.oms.module.bill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.logistics.oms.module.bill.entity.Bill;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BillMapper extends BaseMapper<Bill> {
}
