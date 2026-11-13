package com.cx.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cx.management.entity.AuditFailResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分析失败结果 Mapper
 */
@Mapper
public interface AuditFailResultMapper extends BaseMapper<AuditFailResult> {
}
