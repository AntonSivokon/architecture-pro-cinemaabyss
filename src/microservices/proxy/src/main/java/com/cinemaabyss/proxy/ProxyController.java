package com.cinemaabyss.proxy;

import java.net.URI;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class ProxyController {

    private static final Logger log = LoggerFactory.getLogger(ProxyController.class);
    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            HttpHeaders.CONNECTION.toLowerCase(),
            HttpHeaders.CONTENT_LENGTH.toLowerCase(),
            HttpHeaders.HOST.toLowerCase(),
            HttpHeaders.TRANSFER_ENCODING.toLowerCase());

    private final ProxyProperties properties;
    private final WebClient webClient;

    public ProxyController(ProxyProperties properties, WebClient webClient) {
        this.properties = properties;
        this.webClient = webClient;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse(true);
    }

    @RequestMapping("/**")
    public Mono<Void> proxy(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getRawPath();
        String baseUrl = selectUpstream(path);
        URI target = targetUri(baseUrl, request.getURI().getRawPath(), request.getURI().getRawQuery());
        log.info("Принят запрос на маршрутизацию: {}", target);

        return webClient.method(request.getMethod())
                .uri(target)
                .headers(headers -> copyRequestHeaders(request.getHeaders(), headers))
                .body(BodyInserters.fromDataBuffers(request.getBody()))
                .exchangeToMono(upstream -> {
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(upstream.statusCode());
                    upstream.headers().asHttpHeaders().forEach((name, values) -> {
                        if (!HOP_BY_HOP_HEADERS.contains(name.toLowerCase())) {
                            response.getHeaders().put(name, values);
                        }
                    });
                    return response.writeWith(upstream.bodyToFlux(DataBuffer.class));
                })
                .onErrorResume(error -> {
                    log.warn("Запрос к {} выполнен с ошибкой: {}", target, error.getMessage());
                    exchange.getResponse().setStatusCode(HttpStatus.BAD_GATEWAY);
                    return exchange.getResponse().setComplete();
                });
    }

    private String selectUpstream(String path) {
        return properties.routes().stream()
                .filter(route -> route.matches(path))
                .findFirst()
                .filter(ProxyProperties.Route::selectsService)
                .map(ProxyProperties.Route::serviceUrl)
                .orElse(properties.monolithUrl());
    }

    private static URI targetUri(String baseUrl, String path, String query) {
        String normalizedBase = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        return URI.create(normalizedBase + path + (query == null ? "" : "?" + query));
    }

    private static void copyRequestHeaders(HttpHeaders source, HttpHeaders destination) {
        source.forEach((name, values) -> {
            if (!HOP_BY_HOP_HEADERS.contains(name.toLowerCase())) {
                destination.put(name, values);
            }
        });
    }

    public record HealthResponse(boolean status) {
    }
}