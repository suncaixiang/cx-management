package com.cx.management.util;

import com.cx.management.dto.TradingGroupVO;
import com.cx.management.dto.TradingInvestorVO;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TradingAccountHelper {

    private TradingAccountHelper() {
    }

    public static Map<String, String> buildGroupNameMap(List<TradingGroupVO> groups) {
        Map<String, String> groupNameMap = new LinkedHashMap<>();
        if (groups != null) {
            for (TradingGroupVO group : groups) {
                if (group != null && StringUtils.hasText(group.getGroupId())) {
                    groupNameMap.put(group.getGroupId(), group.getGroupName());
                }
            }
        }
        return groupNameMap;
    }

    public static String resolveGroupName(String groupId, Map<String, String> groupNameMap) {
        if (!StringUtils.hasText(groupId)) {
            return "-";
        }
        String groupName = groupNameMap.get(groupId);
        return StringUtils.hasText(groupName) ? groupName : groupId;
    }

    public static void enrichAccount(TradingInvestorVO account, Map<String, String> groupNameMap) {
        if (account == null) {
            return;
        }
        account.setGroupName(resolveGroupName(account.getGroupId(), groupNameMap));
        // 考核日期仅来自本地启用记录，不从备注猜测补齐
    }
}
