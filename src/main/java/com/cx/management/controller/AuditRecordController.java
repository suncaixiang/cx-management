package com.cx.management.controller;

import com.cx.management.annotation.OperationLog;
import com.cx.management.common.PageResult;
import com.cx.management.common.Result;
import com.cx.management.dto.*;
import com.cx.management.service.AuditRecordFacade;
import com.cx.management.service.AuditTradeRecordFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditRecordController {

    private final AuditRecordFacade auditRecordFacade;


    private final AuditTradeRecordFacade auditTradeRecordFacade;

    @GetMapping("/platforms")
    @PreAuthorize("hasAnyAuthority('audit:record:query', 'audit:record:add', 'audit:disable:query', 'audit:disable:add')")
    public Result<List<AuditPlatformVO>> platforms() {
        return Result.success(auditRecordFacade.listPlatforms());
    }

    @GetMapping("/records")
    @PreAuthorize("hasAuthority('audit:record:query')")
    public Result<PageResult<AuditRecordVO>> pageRecords(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String accountNo,
            @RequestParam(required = false) String platform) {
        return Result.success(auditRecordFacade.pageRecords(current, size, accountNo, platform));
    }

    @GetMapping("/records/{id}")
    @PreAuthorize("hasAuthority('audit:record:query')")
    public Result<AuditRecordVO> getRecord(@PathVariable Long id) {
        return Result.success(auditRecordFacade.getRecord(id));
    }

    @GetMapping("/queryStatus/{id}")
    @PreAuthorize("hasAuthority('audit:record:query')")
    public Result<AuditRecordStatusVO> queryStatus(@PathVariable Long id) {
        return Result.success(auditRecordFacade.queryStatus(id));
    }

    @GetMapping("/startAudit/{id}")
    @PreAuthorize("hasAuthority('audit:record:query')")
    public Result<Boolean> startAudit(@PathVariable Long id) {
        return Result.success(auditRecordFacade.startAudit(id));
    }


    @GetMapping("/records/{id}/trades")
    @PreAuthorize("hasAuthority('audit:record:query')")
    public Result<PageResult<AuditTradeRecordVO>> pageTradeRecords(
            @PathVariable Long id, QueryAuditTradeRecordDTO dto) {
        dto.setAuditRecordId(id);
        return Result.success(auditTradeRecordFacade.queryPageList(dto));
    }

    @GetMapping("/records/{id}/trades/export")
    @PreAuthorize("hasAuthority('audit:record:query')")
    @OperationLog(module = "审核管理", operation = "导出成交明细")
    public void exportTradeRecords(@PathVariable Long id,
                                   QueryAuditTradeRecordDTO dto,
                                   HttpServletResponse response) {
        dto.setAuditRecordId(id);
        auditTradeRecordFacade.exportCsv(dto, response);
    }

    @PostMapping("/records")
    @PreAuthorize("hasAuthority('audit:record:add')")
    @OperationLog(module = "审核管理", operation = "新增审核记录")
    public Result<Long> createRecord(AddAuditRecordDTO dto) {
        return Result.success(auditRecordFacade.createRecord(dto));
    }

    @GetMapping("/disable-products")
    @PreAuthorize("hasAuthority('audit:disable:query')")
    public Result<PageResult<AuditDisableProductVO>> pageDisableProducts(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String productId) {
        return Result.success(auditRecordFacade.pageDisableProducts(current, size, platform, productId));
    }

    @PostMapping("/disable-products")
    @PreAuthorize("hasAuthority('audit:disable:add')")
    @OperationLog(module = "审核管理", operation = "新增禁用产品")
    public Result<AuditDisableProductVO> createDisableProduct(
            @Validated @RequestBody AuditDisableProductRequest request) {
        return Result.success(auditRecordFacade.createDisableProduct(request));
    }

    @DeleteMapping("/disable-products/{id}")
    @PreAuthorize("hasAuthority('audit:disable:delete')")
    @OperationLog(module = "审核管理", operation = "删除禁用产品")
    public Result<Void> deleteDisableProduct(@PathVariable Long id) {
        auditRecordFacade.deleteDisableProduct(id);
        return Result.success();
    }
}
