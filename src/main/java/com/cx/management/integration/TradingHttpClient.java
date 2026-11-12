package com.cx.management.integration;

import com.cx.management.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class TradingHttpClient {

    private final CloseableHttpClient httpClient;

    public TradingHttpClient(@Qualifier("tradingCloseableHttpClient") CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public String get(String url, boolean useUtf8) {
        HttpGet request = new HttpGet(url);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            byte[] body = EntityUtils.toByteArray(response.getEntity());
            if (statusCode >= 400) {
                throw new BusinessException("交易后台返回 HTTP " + statusCode);
            }
            return decode(body, useUtf8);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用交易后台失败: {}", maskPassword(url), e);
            throw new BusinessException("调用交易后台失败: " + e.getMessage());
        }
    }

    private String decode(byte[] body, boolean useUtf8) {
        if (body == null || body.length == 0) {
            throw new BusinessException("交易后台返回空响应");
        }
        // 交易后台常声明 encoding=GB2312，即使请求带 charset=utf8，响应仍可能是 GBK
        String head = new String(body, 0, Math.min(body.length, 256), StandardCharsets.ISO_8859_1);
        String encoding = null;
        int encIdx = head.toLowerCase().indexOf("encoding=");
        if (encIdx >= 0) {
            int q1 = head.indexOf('"', encIdx);
            int q2 = q1 >= 0 ? head.indexOf('"', q1 + 1) : -1;
            if (q1 >= 0 && q2 > q1) {
                encoding = head.substring(q1 + 1, q2).trim();
            }
        }
        if (encoding != null) {
            String upper = encoding.toUpperCase();
            if (upper.contains("GB2312") || upper.contains("GBK")) {
                return new String(body, Charset.forName("GBK"));
            }
            if (upper.contains("UTF")) {
                return new String(body, StandardCharsets.UTF_8);
            }
        }
        Charset charset = useUtf8 ? StandardCharsets.UTF_8 : Charset.forName("GBK");
        return new String(body, charset);
    }

    private String maskPassword(String url) {
        return url.replaceAll("sapass=[^&]*", "sapass=***");
    }
}
