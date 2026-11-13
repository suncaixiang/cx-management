package com.cx.management.service;

import com.cx.management.entity.AuditRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 审核发布器
 *
 * @author suncaixiang
 */
@Slf4j
@Component
public class AuditPublisher {

    @Resource
    private ApplicationEventPublisher eventPublisher;


    /**
     * 发布审核启动事件
     *
     * @param event 事件内容
     */
    public void publishStartAudit(AuditRecord event) {
        try {
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.info("发布审核启动事件异常，id:{},msg:{}", event.getId(), e.getMessage(), e);
        }
    }

    public void onCheckAuditDone(Long auditRecordId) {
        try {
            eventPublisher.publishEvent(auditRecordId);
        } catch (Exception e) {
            log.info("发布检查是否完成，id:{},msg:{}", auditRecordId, e.getMessage(), e);
        }
    }


}
