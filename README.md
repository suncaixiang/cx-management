# CX Management 后台管理系统

基于 Spring Boot 2.7 + Java 11 + MySQL 8 + MyBatis-Plus 的 RBAC 权限管理后端。

## 技术栈

- Spring Boot 2.7.18
- Spring Security + JWT
- MyBatis-Plus 3.5.5
- MySQL 8

## 功能特性

- RBAC 权限模型（用户、角色、权限、关联表）
- JWT 无状态登录认证
- 用户/角色 CRUD
- 权限菜单树
- 登录日志 & 操作日志 AOP
- 统一返回 `Result<T>`

## 快速开始

### 1. 创建数据库

```bash
mysql -u root -p < src/main/resources/sql/schema.sql
```

### 2. 修改配置

编辑 `src/main/resources/application.yml`，配置数据库连接信息。

### 3. 启动项目

```bash
mvn spring-boot:run
```

服务默认运行在 `http://localhost:8080`

### 默认账号

- 用户名: `admin`
- 密码: `admin123`

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/login | 登录 |
| POST | /api/auth/logout | 退出 |
| GET | /api/auth/info | 当前用户信息 |
| GET | /api/users | 用户分页列表 |
| POST | /api/users | 新增用户 |
| PUT | /api/users/{id} | 修改用户 |
| DELETE | /api/users/{id} | 删除用户 |
| GET | /api/roles | 角色分页列表 |
| POST | /api/roles | 新增角色 |
| PUT | /api/roles/{id} | 修改角色 |
| DELETE | /api/roles/{id} | 删除角色 |
| GET | /api/roles/permissions/tree | 权限树 |
| GET | /api/logs/login | 登录日志 |
| GET | /api/logs/operation | 操作日志 |

## 项目结构

```
src/main/java/com/cx/management/
├── annotation/       # 自定义注解
├── aspect/           # AOP 切面
├── common/           # 通用类
├── config/           # 配置类
├── controller/       # 控制器
├── dto/              # 数据传输对象
├── entity/           # 实体类
├── mapper/           # MyBatis Mapper
├── security/         # 安全相关
└── service/          # 业务逻辑
```
