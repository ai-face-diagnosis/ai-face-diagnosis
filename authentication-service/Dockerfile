# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Копируем pom и скачиваем зависимости
COPY pom.xml .
RUN mkdir src && echo " " > src/placeholder.txt
RUN mvn dependency:go-offline -B

# Копируем исходники и билдим
COPY src ./src
RUN mvn clean package -DskipTests

# Финальный образ
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
