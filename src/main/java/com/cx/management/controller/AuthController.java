package com.cx.management.controller;

import com.cx.management.common.Result;
import com.cx.management.dto.LoginRequest;
import com.cx.management.dto.LoginResponse;
import com.cx.management.dto.UserInfoVO;
import com.cx.management.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                       HttpServletRequest httpRequest) {
        return Result.success(authService.login(request, httpRequest));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest httpRequest) {
        authService.logout(httpRequest);
        return Result.success();
    }

    @GetMapping("/info")
    public Result<UserInfoVO> getCurrentUser() {
        return Result.success(authService.getCurrentUser());
    }
}
