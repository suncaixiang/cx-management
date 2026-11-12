package com.cx.management.dto;

import lombok.Data;

import java.util.List;

@Data
public class LoginResponse {

    private String token;
    private Long userId;
    private String username;
    private List<String> roles;
    private List<String> permissions;
}
