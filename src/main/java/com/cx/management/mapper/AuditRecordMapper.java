package com.cx.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cx.management.entity.AuditRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审核记录 Mapper
 */
@Mapper
public interface AuditRecordMapper extends BaseMapper<AuditRecord> {
}
