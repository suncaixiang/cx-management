package com.cx.management.config;

import lombok.RequiredArgsConstructor;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.conscrypt.Conscrypt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.Security;
import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
public class TradingHttpClientConfig {

    private static final String[] TLS_PROTOCOLS = { "TLSv1.2" };
    private static final int MAX_TOTAL = 64;
    private static final int MAX_PER_ROUTE = 32;

    private final TradingApiProperties tradingApiProperties;

    static {
        Security.insertProviderAt(Conscrypt.newProvider(), 1);
    }

    @Bean(destroyMethod = "close")
    public CloseableHttpClient tradingCloseableHttpClient() throws Exception {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(tradingApiProperties.getConnectTimeout())
                .setSocketTimeout(tradingApiProperties.getReadTimeout())
                .build();

        if (!tradingApiProperties.isSslEnabled()) {
            PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
            cm.setMaxTotal(MAX_TOTAL);
            cm.setDefaultMaxPerRoute(MAX_PER_ROUTE);
            return HttpClients.custom()
                    .setConnectionManager(cm)
                    .setDefaultRequestConfig(requestConfig)
                    .evictExpiredConnections()
                    .evictIdleConnections(30, TimeUnit.SECONDS)
                    .build();
        }

        SSLContext sslContext = SSLContext.getInstance("TLS", "Conscrypt");
        sslContext.init(null, new TrustManager[] { trustAllManager() }, null);

        SSLConnectionSocketFactory socketFactory = tradingApiProperties.isSslVerify()
                ? new SSLConnectionSocketFactory(sslContext, TLS_PROTOCOLS, null,
                SSLConnectionSocketFactory.getDefaultHostnameVerifier())
                : new SSLConnectionSocketFactory(sslContext, TLS_PROTOCOLS, null,
                NoopHostnameVerifier.INSTANCE);

        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", socketFactory)
                .build();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
        cm.setMaxTotal(MAX_TOTAL);
        cm.setDefaultMaxPerRoute(MAX_PER_ROUTE);

        return HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .evictIdleConnections(30, TimeUnit.SECONDS)
                .build();
    }

    private X509TrustManager trustAllManager() {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
            }

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[0];
            }
        };
    }
}
