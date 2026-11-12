package com.cx.management.aspect;

import com.cx.management.annotation.OperationLog;
import com.cx.management.common.IpUtils;
import com.cx.management.common.RequestUtils;
import com.cx.management.entity.SysOperationLog;
import com.cx.management.security.SecurityUtils;
import com.cx.management.service.SysLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final SysLogService logService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint point, OperationLog operationLog) throws Throwable {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            if ("GET".equalsIgnoreCase(request.getMethod())) {
                return point.proceed();
            }
        }
        if (operationLog.operation().contains("查询")) {
            return point.proceed();
        }

        SysOperationLog logEntity = new SysOperationLog();
        logEntity.setModule(operationLog.module());
        logEntity.setOperation(operationLog.operation());
        logEntity.setUserId(SecurityUtils.getUserId());
        logEntity.setUsername(SecurityUtils.getUsername());

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            logEntity.setIp(IpUtils.getClientIp(request));
            logEntity.setMethod(RequestUtils.getRequestMethod(request));
        }

        try {
            String params = Arrays.stream(point.getArgs())
                    .filter(arg -> !(arg instanceof HttpServletRequest))
                    .map(arg -> {
                        try {
                            return objectMapper.writeValueAsString(arg);
                        } catch (Exception e) {
                            return String.valueOf(arg);
                        }
                    })
                    .collect(Collectors.joining(", "));
            if (params.length() > 2000) {
                params = params.substring(0, 2000);
            }
            logEntity.setParams(params);
        } catch (Exception e) {
            logEntity.setParams("参数序列化失败");
        }

        try {
            return point.proceed();
        } finally {
            logService.save(logEntity);
        }
    }
}
