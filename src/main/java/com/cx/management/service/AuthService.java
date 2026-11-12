package com.cx.management.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cx.management.common.IpUtils;
import com.cx.management.common.RequestUtils;
import com.cx.management.common.exception.BusinessException;
import com.cx.management.dto.*;
import com.cx.management.entity.SysOperationLog;
import com.cx.management.entity.SysPermission;
import com.cx.management.entity.SysUser;
import com.cx.management.mapper.SysPermissionMapper;
import com.cx.management.mapper.SysUserMapper;
import com.cx.management.security.JwtUtils;
import com.cx.management.security.LoginUser;
import com.cx.management.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final SysUserMapper userMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysLogService logService;

    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            String token = jwtUtils.generateToken(loginUser.getUserId(), loginUser.getUsername());

            saveLoginLog(loginUser.getUserId(), request.getUsername(), httpRequest, "登录");

            LoginResponse response = new LoginResponse();
            response.setToken(token);
            response.setUserId(loginUser.getUserId());
            response.setUsername(loginUser.getUsername());
            response.setRoles(loginUser.getRoles());
            response.setPermissions(loginUser.getPermissions());
            return response;
        } catch (BadCredentialsException e) {
            saveLoginLog(null, request.getUsername(), httpRequest, "登录");
            throw e;
        } catch (Exception e) {
            saveLoginLog(null, request.getUsername(), httpRequest, "登录");
            throw new BusinessException(e.getMessage());
        }
    }

    public void logout(HttpServletRequest httpRequest) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser != null) {
            saveLoginLog(loginUser.getUserId(), loginUser.getUsername(), httpRequest, "退出登录");
        }
    }

    private void saveLoginLog(Long userId, String username, HttpServletRequest request, String operation) {
        SysOperationLog log = new SysOperationLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setModule("系统");
        log.setOperation(operation);
        log.setMethod(RequestUtils.getRequestMethod(request));
        log.setParams("{\"username\":\"" + username + "\"}");
        log.setIp(IpUtils.getClientIp(request));
        logService.save(log);
    }

    public UserInfoVO getCurrentUser() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new BusinessException(401, "未登录");
        }
        SysUser user = loginUser.getUser();
        List<SysPermission> permissions = permissionMapper.selectByUserId(user.getId());
        List<MenuVO> menus = buildMenuTree(includeParentMenus(permissions));

        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRoles(loginUser.getRoles());
        vo.setPermissions(loginUser.getPermissions());
        vo.setMenus(menus);
        return vo;
    }

    public List<MenuVO> getMenuTree() {
        List<SysPermission> all = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>()
                        .eq(SysPermission::getStatus, 1)
                        .in(SysPermission::getType, 1, 2)
                        .orderByAsc(SysPermission::getSort));
        return buildMenuTree(all);
    }

    public List<MenuVO> getPermissionTree() {
        List<SysPermission> all = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>()
                        .eq(SysPermission::getStatus, 1)
                        .orderByAsc(SysPermission::getSort));
        return buildMenuTree(all);
    }

    private List<MenuVO> buildMenuTree(List<SysPermission> permissions) {
        List<SysPermission> menuPermissions = permissions.stream()
                .filter(p -> p.getType() != null && (p.getType() == 1 || p.getType() == 2))
                .collect(Collectors.toList());

        Map<Long, MenuVO> map = menuPermissions.stream()
                .map(this::toMenuVO)
                .collect(Collectors.toMap(MenuVO::getId, m -> m));
        List<MenuVO> roots = new ArrayList<>();
        for (MenuVO menu : map.values()) {
            if (menu.getParentId() == null || menu.getParentId() == 0) {
                roots.add(menu);
            } else {
                MenuVO parent = map.get(menu.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(menu);
                }
            }
        }
        sortMenus(roots);
        return roots;
    }

    private List<SysPermission> includeParentMenus(List<SysPermission> permissions) {
        Set<Long> ids = permissions.stream().map(SysPermission::getId).collect(Collectors.toSet());
        List<SysPermission> result = new ArrayList<>(permissions);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (SysPermission permission : new ArrayList<>(result)) {
                Long parentId = permission.getParentId();
                if (parentId != null && parentId > 0 && !ids.contains(parentId)) {
                    SysPermission parent = permissionMapper.selectById(parentId);
                    if (parent != null && parent.getDeleted() != null && parent.getDeleted() == 0) {
                        result.add(parent);
                        ids.add(parentId);
                        changed = true;
                    }
                }
            }
        }
        return result;
    }

    private void sortMenus(List<MenuVO> menus) {
        menus.sort(Comparator.comparing(MenuVO::getSort, Comparator.nullsLast(Integer::compareTo)));
        for (MenuVO menu : menus) {
            if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
                sortMenus(menu.getChildren());
            }
        }
    }

    private MenuVO toMenuVO(SysPermission p) {
        MenuVO vo = new MenuVO();
        vo.setId(p.getId());
        vo.setParentId(p.getParentId());
        vo.setName(p.getName());
        vo.setPermission(p.getPermission());
        vo.setType(p.getType());
        vo.setPath(p.getPath());
        vo.setComponent(p.getComponent());
        vo.setIcon(p.getIcon());
        vo.setSort(p.getSort());
        return vo;
    }
}
