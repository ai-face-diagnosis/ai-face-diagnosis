# syntax=docker/dockerfile:1

# 1. Сборка проекта
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Копируем pom.xml и скачиваем зависимости
COPY pom.xml .
RUN mkdir src && echo " " > src/placeholder.txt
RUN mvn dependency:go-offline -B

# Копируем исходники и собираем JAR
COPY src ./src
RUN mvn clean package -DskipTests

# 2. Финальный образ для запуска
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Копируем JAR из стадии сборки
COPY --from=build /app/target/*.jar app.jar

# Настройка порта Spring Boot
EXPOSE 8084

# Передаем переменные окружения для конфигурации (опционально)
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8084

# Запуск приложения
ENTRYPOINT ["java", "-jar", "app.jar"]

