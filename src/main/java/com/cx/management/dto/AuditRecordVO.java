package com.cx.management.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditRecordVO {

    private Long id;

    private String accountNo;

    private String platform;

    private String platformLabel;

    private String tradeStartDate;

    private String tradeEndDate;

    private Integer tradeNum;

    private Integer status;

    private String statusLabel;


    private LocalDateTime created;

    private LocalDateTime modified;
}
