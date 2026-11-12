package com.cx.management.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "trading.api")
public class TradingApiProperties {

    /** 交易后台 IP 或域名 */
    private String host = "127.0.0.1";

    /** querylog 等查询接口端口（端口2，TLS1.2） */
    private int port = 8443;

    /** 开户、出入金等操作接口端口（端口1） */
    private int port1 = 17803;

    /** 是否使用 HTTPS */
    private boolean sslEnabled = true;

    /** 管理员账号 */
    private String sa = "";

    /** 管理员密码 */
    private String sapass = "";

    /** 连接超时（毫秒） */
    private int connectTimeout = 10000;

    /** 读取超时（毫秒） */
    private int readTimeout = 30000;

    /** 是否校验 SSL 证书（内网自签证书可关闭） */
    private boolean sslVerify = false;

    /** 回收后账号名称（空字符串表示清空名称） */
    private String recycleName = "";

    /** 回收后账号密码 */
    private String recyclePassword = "1234";
}
