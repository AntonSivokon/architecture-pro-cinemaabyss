package com.cinemaabyss.proxy;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cinemaabyss.proxy")
public record ProxyProperties(String monolithUrl, List<Route> routes) {

    public ProxyProperties {
        requireAbsoluteUrl("monolith-url", monolithUrl);
        routes = routes == null
                ? List.of()
                : routes.stream()
                        .sorted(Comparator.comparingInt((Route route) -> route.path().length()).reversed())
                        .toList();
        if (routes.stream().map(Route::path).distinct().count() != routes.size()) {
            throw new IllegalArgumentException("Пути маршрутов должны быть уникальными");
        }
    }

    private static void requireAbsoluteUrl(String name, String value) {
        URI uri = URI.create(value);
        if (uri.getScheme() == null || uri.getHost() == null) {
            throw new IllegalArgumentException(name + " должен быть абсолютным URL-адресом");
        }
    }

    public record Route(String path, String serviceUrl, boolean gradualMigration, int migrationPercent) {

        public Route {
            if (path == null || !path.startsWith("/")) {
                throw new IllegalArgumentException("путь маршрута должен начинаться с /");
            }
            while (path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            requireAbsoluteUrl("route service-url", serviceUrl);
            if (migrationPercent < 0 || migrationPercent > 100) {
                throw new IllegalArgumentException("Параметр route migration-percent должен находиться в диапазоне от 0 до 100");
            }
        }

        public boolean matches(String requestPath) {
            return path.equals("/")
                    || requestPath.equals(path)
                    || requestPath.startsWith(path + "/");
        }

        public boolean selectsService() {
            return !gradualMigration || ThreadLocalRandom.current().nextInt(100) < migrationPercent;
        }
    }
}