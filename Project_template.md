## Задание 1

[Диаграмма контейнеров CinemaAbyss](diagrams/container/CinemaAbyss_Container.puml)

## Задание 2

### 1. Proxy
Реализован [прокси-сервис](src/microservices/proxy) (API Gateway) на Java. Механизм маршрутизации работает по стратегии "Случайного распределения" с указанием вероятности попадания на сервис `movies`.

При установке `MOVIES_MIGRATION_PERCENT` в 0, все запросы маршрутизируются на legacy сервис `monolith`.

При установке `MOVIES_MIGRATION_PERCENT` в 100, все запросы маршрутизируются на новый микросервис сервис `movies`.

### 2. Kafka

Реализован [сервис событий](src/microservices/events) на Java.

- **Скриншот успешного выполнения тестов**: 

![tests_result.png](images/task2/tests_result.png)

- **Скриншоты состояния топиков Kafka**:
  - Общее состояние топиков:

    ![all_topics_state.png](images/task2/all_topics_state.png)
  - Сообщение в топике movie-events:

    ![movie_topic_message.png](images/task2/movie_topic_message.png)

  - Сообщение в топике payment-events:

    ![payment_topic_message.png](images/task2/payment_topic_message.png)

  - Сообщение в топике user-events:

    ![user_topic_message.png](images/task2/user_topic_message.png)

## Задание 3

### CI/CD

**Скриншот успешной сборки и тестов:**

![workflows.png](images/task3/workflows.png)

### Proxy в Kubernetes

**Скриншот вызова https://cinemaabyss.example.com/api/movies:**

![get_movies.png](images/task3/get_movies.png)

**Скриншот выполнения тестов:**

![test_result.png](images/task3/test_result.png)

**Скриншот работы events-service после тестов:**

![events_service_logs.png](images/task3/events_service_logs.png)

## Задание 4

**Скриншот развертывания helm:**

![helm_init.png](images/task4/helm_init.png)

**Скриншот вывода https://cinemaabyss.example.com/api/movies:**

![helm_result.png](images/task4/helm_result.png)

# Задание 5

**Скриншот работы circuit breaker:**

![circuit_breaker_result.png](images/task5/circuit_breaker_result.png)