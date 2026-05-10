package com.cobre.notification.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Value("${webhook.client.connect-timeout-ms:3000}")
    private int connectTimeoutMs;

    @Value("${webhook.client.response-timeout-ms:5000}")
    private int responseTimeoutMs;

    @Value("${webhook.client.max-in-memory-size-mb:1}")
    private int maxInMemorySizeMb;

    @Bean
    public WebClient webhookWebClient() {
        SslContext sslContext;
        try {
            sslContext = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
        } catch (SSLException e) {
            throw new IllegalStateException("Failed to build SSL context for webhook client", e);
        }

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(responseTimeoutMs))
                .secure(ssl -> ssl.sslContext(sslContext));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Cobre-Source", "notification-service")
                .defaultHeader("X-Cobre-Version", "1.0")
                .codecs(config -> config.defaultCodecs()
                        .maxInMemorySize(maxInMemorySizeMb * 1024 * 1024))
                .build();
    }
}
