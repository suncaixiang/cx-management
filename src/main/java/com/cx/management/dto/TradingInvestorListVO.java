package com.cx.management.dto;

import lombok.Data;

import java.util.List;

@Data
public class TradingInvestorListVO {

    private List<TradingInvestorVO> accounts;

    private List<TradingGroupVO> groups;

    private int total;
}
