package com.cx.management.dto;

import lombok.Data;

@Data
public class AccountRecycleItemResultVO {

    private String account;

    private boolean success;

    private String message;
}
