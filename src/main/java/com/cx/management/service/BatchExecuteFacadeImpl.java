package com.cx.management.service;

import com.cx.management.entity.AuditTradeRecord;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * @author suncaixiang
 */
@Slf4j
@Service
public class BatchExecuteFacadeImpl implements BatchExecuteFacade {


    @Resource
    private AuditTradeRecordFacade auditTradeRecordFacade;

    @Resource
    private ExecutorService commonExecutorService;


    @Override
    public void analysis(List<AuditTradeRecord> auditTradeRecordDOList) {
        if (CollectionUtils.isEmpty(auditTradeRecordDOList)) {
            return;
        }
        //100个分批执行
        List<List<AuditTradeRecord>> listList = Lists.partition(auditTradeRecordDOList, 100);
        for (List<AuditTradeRecord> processList : listList) {
            // 等待所有线程执行完成
            CompletableFuture.allOf(processList.stream().map(auditTradeRecordDO ->
                    CompletableFuture.runAsync(() -> {
                        try {
                            auditTradeRecordFacade.analysis(auditTradeRecordDO);
                        } catch (Exception e) {
                            log.error("分析审核成交明细异常,recordId:{},tradeId:{},msg:{}", auditTradeRecordDO.getAuditRecordId(), auditTradeRecordDO.getId(), e.getMessage(), e);
                        }
                    }, commonExecutorService)
            ).toArray(CompletableFuture[]::new)).join();
        }

    }

}
