package com.cx.management.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

@Data
public class AccountRecycleRequestDTO {

    @NotEmpty(message = "账号列表不能为空")
    private List<String> accounts;

    /** 账号 → 风控日志原文，用于回收成功后判定本地状态 */
    private Map<String, String> riskContents;

    /** 回收后写入交易账户的备注，默认 yyyyMMdd回收 */
    private String remark;
}
