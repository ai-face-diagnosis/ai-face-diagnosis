# Используем официальный образ Maven для сборки
FROM maven:3.9.5-eclipse-temurin-17 AS build

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем pom.xml и скачиваем зависимости
COPY pom.xml .
RUN mvn dependency:go-offline

# Копируем исходники
COPY src ./src

# Собираем проект
RUN mvn clean package -DskipTests

# Используем легковесный образ для запуска
FROM eclipse-temurin:17-jre

# Рабочая директория в контейнере
WORKDIR /app

# Копируем собранный jar из стадии сборки
COPY --from=build /app/target/common-dto-0.0.1-SNAPSHOT.jar app.jar

# Команда запуска приложения
ENTRYPOINT ["java", "-jar", "app.jar"]

