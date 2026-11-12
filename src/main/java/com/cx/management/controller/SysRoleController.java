package com.cx.management.controller;

import com.cx.management.annotation.OperationLog;
import com.cx.management.common.PageResult;
import com.cx.management.common.Result;
import com.cx.management.dto.MenuVO;
import com.cx.management.dto.RoleDTO;
import com.cx.management.entity.SysRole;
import com.cx.management.service.AuthService;
import com.cx.management.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;
    private final AuthService authService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<PageResult<SysRole>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String roleName) {
        return Result.success(roleService.page(current, size, roleName));
    }

    @GetMapping("/all")
    public Result<List<SysRole>> listAll() {
        return Result.success(roleService.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:query')")
    public Result<SysRole> getById(@PathVariable Long id) {
        return Result.success(roleService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:role:add')")
    @OperationLog(module = "角色管理", operation = "新增角色")
    public Result<Void> create(@Valid @RequestBody RoleDTO dto) {
        roleService.create(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:edit')")
    @OperationLog(module = "角色管理", operation = "修改角色")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody RoleDTO dto) {
        dto.setId(id);
        roleService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:delete')")
    @OperationLog(module = "角色管理", operation = "删除角色")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.success();
    }

    @GetMapping("/permissions/tree")
    @PreAuthorize("hasAuthority('system:role:query')")
    public Result<List<MenuVO>> permissionTree() {
        return Result.success(authService.getPermissionTree());
    }
}
