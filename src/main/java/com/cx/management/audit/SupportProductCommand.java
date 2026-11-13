package com.cx.management.audit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cx.management.entity.AuditDisableProduct;
import com.cx.management.entity.AuditTradeRecord;
import com.cx.management.enums.RuleTypeEnum;
import com.cx.management.enums.TradeCombOffsetEnum;
import com.cx.management.mapper.AuditDisableProductMapper;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 不可交易品种行为
 *
 * @author suncaixiang
 */
public class SupportProductCommand implements AuditRuleCommand {

    private AuditDisableProductMapper auditDisableProductMapper;

    public SupportProductCommand(AuditDisableProductMapper auditDisableProductMapper) {
        this.auditDisableProductMapper = auditDisableProductMapper;
    }

    @Override
    public AuditRuleExecuteResult execute(AuditTradeRecord auditTradeRecordDO) {
        if (!TradeCombOffsetEnum.OPEN.getCode().equals(auditTradeRecordDO.getTradeCombOffset())) {
            return AuditRuleExecuteResult.success(RuleTypeEnum.SUPPORT_PRODUCT_LIMIT.getCode());
        }

        //是否支持
        if (support(auditTradeRecordDO.getPlatform(), auditTradeRecordDO.getProductId())) {
            return AuditRuleExecuteResult.success(RuleTypeEnum.SUPPORT_PRODUCT_LIMIT.getCode());
        }
        return AuditRuleExecuteResult.fail(RuleTypeEnum.SUPPORT_PRODUCT_LIMIT.getCode(), "不可交易品种" + auditTradeRecordDO.getProductId());
    }

    public Boolean support(String platform, String productId) {
        List<String> notSupportProductList = getNotSupportProduct(platform);
        return !notSupportProductList.contains(productId);
    }


    private List<String> getNotSupportProduct(String platform) {
        LambdaQueryWrapper<AuditDisableProduct> wrapper = new LambdaQueryWrapper<AuditDisableProduct>()
                .eq(AuditDisableProduct::getPlatform, platform);
        List<AuditDisableProduct> auditDisableProductDOList = auditDisableProductMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(auditDisableProductDOList)) {
            return Lists.newArrayList();
        }
        return auditDisableProductDOList.stream().map(AuditDisableProduct::getProductId).collect(Collectors.toList());
    }


}
