package com.cx.management.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cx.management.audit.*;
import com.cx.management.common.PageResult;
import com.cx.management.dto.AuditTradeRecordVO;
import com.cx.management.dto.QueryAuditTradeRecordDTO;
import com.cx.management.entity.AuditFailResult;
import com.cx.management.entity.AuditTradeRecord;
import com.cx.management.common.exception.BusinessException;
import com.cx.management.enums.AuditTradeRecordStatusEnum;
import com.cx.management.mapper.AuditDisableProductMapper;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author suncaixiang
 */
@Slf4j
@Service
public class AuditTradeRecordFacadeImpl implements AuditTradeRecordFacade {

    @Resource
    private AuditTradeRecordService auditTradeRecordService;

    @Resource
    private AuditFailResultService auditFailResultService;

    @Resource
    private AuditPublisher auditPublisher;

    @Resource
    private AuditDisableProductMapper auditDisableProductMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void analysis(AuditTradeRecord auditTradeRecordDO) {
        if (!AuditTradeRecordStatusEnum.INIT.getCode().equals(auditTradeRecordDO.getStatus())) {
            return;
        }
        //状态更新失败，不作处理
        Boolean result = auditTradeRecordService.updateStatus(auditTradeRecordDO.getId(), AuditTradeRecordStatusEnum.INIT.getCode(), AuditTradeRecordStatusEnum.DOING.getCode());
        if (!result) {
            //发布审核完成检查指令
            auditPublisher.onCheckAuditDone(auditTradeRecordDO.getAuditRecordId());
            return;
        }

        //规则链
        AuditRuleCommandChain auditRuleCommandChain = new AuditRuleCommandChain();
        auditRuleCommandChain.add(new ClosePriceCommand());
        auditRuleCommandChain.add(new MainFuturesCommand());
        auditRuleCommandChain.add(new OpenPriceCommand());
        auditRuleCommandChain.add(new SupportProductCommand(auditDisableProductMapper));
        auditRuleCommandChain.add(new TradeTimeCommand());
        //执行，全部成功
        List<AuditRuleExecuteResult> auditRuleExecuteResultList = auditRuleCommandChain.execute(auditTradeRecordDO);
        if (auditRuleExecuteResultList.stream().allMatch(AuditRuleExecuteResult::getSuccess)) {
            auditTradeRecordDO.setStatus(AuditTradeRecordStatusEnum.PASS.getCode());
            auditTradeRecordDO.setModified(LocalDateTime.now());
            auditTradeRecordService.updateById(auditTradeRecordDO);
            return;
        } else {
            //保存失败结果
            List<AuditFailResult> auditFailResultDOList = auditFailResultDOList(auditRuleExecuteResultList, auditTradeRecordDO);
            auditFailResultService.saveBatch(auditFailResultDOList);

            //更新为失败
            auditTradeRecordDO.setStatus(AuditTradeRecordStatusEnum.FAIL.getCode());
            auditTradeRecordDO.setModified(LocalDateTime.now());
            auditTradeRecordService.updateById(auditTradeRecordDO);
        }

        //发布审核完成检查指令
        auditPublisher.onCheckAuditDone(auditTradeRecordDO.getAuditRecordId());
    }


    private List<AuditFailResult> auditFailResultDOList(List<AuditRuleExecuteResult> auditRuleExecuteResultList, AuditTradeRecord auditTradeRecordDO) {
        return auditRuleExecuteResultList.stream().filter(s -> !s.getSuccess()).map(s -> {
            AuditFailResult auditFailResultDO = new AuditFailResult();
            auditFailResultDO.setAuditRecordId(auditTradeRecordDO.getAuditRecordId());
            auditFailResultDO.setTradeRecordId(auditTradeRecordDO.getId());
            auditFailResultDO.setTriggerRuleType(s.getRuleType());
            auditFailResultDO.setTriggerDetail(s.getMessage());
            auditFailResultDO.setTransactionNum(auditTradeRecordDO.getTransactionNum());
            return auditFailResultDO;
        }).collect(Collectors.toList());
    }


    @Override
    public PageResult<AuditTradeRecordVO> queryPageList(QueryAuditTradeRecordDTO dto) {

        //查询完整记录数据
        Page<AuditTradeRecord> page = auditTradeRecordService.queryPage(dto);
        if (CollectionUtils.isEmpty(page.getRecords())) {
            return new PageResult<>(Lists.newArrayList(), page.getTotal(), page.getCurrent(), page.getSize());
        }

        List<AuditTradeRecordVO> records = toTradeVoList(page.getRecords());
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public void exportCsv(QueryAuditTradeRecordDTO dto, HttpServletResponse response) {
        if (dto == null || dto.getAuditRecordId() == null) {
            throw new BusinessException("审核记录ID不能为空");
        }
        List<AuditTradeRecord> tradeList = auditTradeRecordService.queryList(dto);
        List<AuditTradeRecordVO> records = toTradeVoList(tradeList);
        String fileName = "audit_trades_" + dto.getAuditRecordId() + "_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".csv";
        try {
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("text/csv;charset=UTF-8");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()) + "\"");
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
                // Excel 兼容 BOM
                writer.write('\ufeff');
                writer.println("账号,合约,产品,交易日,成交时间,手数,成交价,买卖,开平,平仓收益,成交序号,状态,触发详情");
                for (AuditTradeRecordVO row : records) {
                    writer.println(String.join(",",
                            csv(row.getAccountNo()),
                            csv(row.getInstrumentId()),
                            csv(row.getProductId()),
                            csv(row.getTradingDay()),
                            csv(row.getTradeTime()),
                            csv(row.getTradeVolume()),
                            csv(row.getTradePrice()),
                            csv(row.getTradeDirection()),
                            csv(row.getTradeCombOffset()),
                            csv(row.getCloseProfit()),
                            csv(row.getTransactionNum()),
                            csv(AuditTradeRecordStatusEnum.labelOf(row.getStatus())),
                            csv(row.getTriggerDetail())
                    ));
                }
                writer.flush();
            }
            log.info("导出成交明细完成, auditRecordId:{}, status:{}, size:{}",
                    dto.getAuditRecordId(), dto.getStatus(), records.size());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("导出成交明细失败, auditRecordId:{}", dto.getAuditRecordId(), ex);
            throw new BusinessException("导出失败：" + ex.getMessage());
        }
    }

    private List<AuditTradeRecordVO> toTradeVoList(List<AuditTradeRecord> tradeList) {
        if (CollectionUtils.isEmpty(tradeList)) {
            return Lists.newArrayList();
        }
        Set<Long> tradeRecordIdList = tradeList.stream().map(AuditTradeRecord::getId).collect(Collectors.toSet());
        List<AuditFailResult> auditFailResultDOList = auditFailResultService.query(tradeRecordIdList, null);
        Map<Long, List<AuditFailResult>> groupByMap = CollectionUtils.isEmpty(auditFailResultDOList)
                ? Collections.emptyMap()
                : auditFailResultDOList.stream().collect(Collectors.groupingBy(AuditFailResult::getTradeRecordId));
        return tradeList.stream().map(auditTradeRecordDO -> {
            AuditTradeRecordVO vo = auditTradeRecordDO.clone(AuditTradeRecordVO.class);
            List<AuditFailResult> failList = groupByMap.get(auditTradeRecordDO.getId());
            if (!CollectionUtils.isEmpty(failList)) {
                vo.setTriggerDetail(failList.stream()
                        .map(AuditFailResult::getTriggerDetail)
                        .collect(Collectors.joining("\n")));
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        boolean needQuote = text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r");
        if (text.contains("\"")) {
            text = text.replace("\"", "\"\"");
        }
        return needQuote ? "\"" + text + "\"" : text;
    }

}
