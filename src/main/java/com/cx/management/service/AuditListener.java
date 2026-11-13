package com.cx.management.service;

import com.cx.management.entity.AuditRecord;
import com.cx.management.entity.AuditTradeRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 审核监听器
 *
 * @author suncaixiang
 */
@Slf4j
@Component
public class AuditListener {

    @Resource
    private AuditRecordFacade auditRecordFacade;

    @Resource
    private AuditTradeRecordFacade auditTradeRecordFacade;


    /**
     * 监听审核启动事件
     *
     * @param auditRecordDO 事件
     */
    @EventListener
    public void onAuditRecord(AuditRecord auditRecordDO) {
        try {
            auditRecordFacade.startAudit(auditRecordDO);
        } catch (Exception e) {
            log.error("监听审核启动事件异常，id:{},msg:{}", auditRecordDO.getId(), e.getMessage(), e);
        }
    }


    /**
     * 监听审核交易启动事件
     *
     * @param auditTradeRecordDO 事件
     */
    @EventListener
    public void onAuditTradeRecord(AuditTradeRecord auditTradeRecordDO) {
        try {
            auditTradeRecordFacade.analysis(auditTradeRecordDO);
        } catch (Exception e) {
            log.error("监听审核交易启动事件异常，id:{},msg:{}", auditTradeRecordDO.getId(), e.getMessage(), e);
        }
    }

    @EventListener
    public void onCheckAuditDone(Long auditRecordId) {
        try {
            log.info("检查当前审核是否完成auditRecordId:{}", auditRecordId);
            //以免数据未保存成功
            Thread.sleep(1000);
            auditRecordFacade.checkAuditDone(auditRecordId);
        } catch (Exception e) {
            log.error("监听是否审核完成，id:{},msg:{}", auditRecordId, e.getMessage(), e);
        }
    }

}
