package com.cinemaabyss.proxy;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(ProxyProperties.class)
public class ProxyConfiguration {

    @Bean
    WebClient webClient() {
        return WebClient.builder().build();
    }
}