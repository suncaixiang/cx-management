package com.cx.management.controller;

import com.cx.management.common.PageResult;
import com.cx.management.common.Result;
import com.cx.management.entity.SysOperationLog;
import com.cx.management.service.SysLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class SysLogController {

    private final SysLogService logService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:log:list')")
    public Result<PageResult<SysOperationLog>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username) {
        return Result.success(logService.page(current, size, username));
    }
}
