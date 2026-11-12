package com.cx.management.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AccountRecycleResultVO {

    private int successCount;

    private int failCount;

    private List<AccountRecycleItemResultVO> results = new ArrayList<>();
}
