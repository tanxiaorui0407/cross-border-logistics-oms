package com.logistics.oms.module.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.logistics.oms.module.common.entity.SysSeq;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 发号器：UPDATE 行锁递增 + SELECT 回读，事务内 read-your-writes。
 */
@Mapper
public interface SysSeqMapper extends BaseMapper<SysSeq> {

    @Update("UPDATE sys_seq SET seq_value = seq_value + 1, updated_at = CURRENT_TIMESTAMP WHERE name = #{name}")
    int increment(@Param("name") String name);

    @Select("SELECT seq_value FROM sys_seq WHERE name = #{name}")
    Long selectValue(@Param("name") String name);
}
