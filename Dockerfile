# Сборка
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -e -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -e -B package -DskipTests

# Прод
FROM eclipse-temurin:17-jre
WORKDIR /app

# jar
COPY --from=build /app/target/coralclub-bot-1.0.0-shaded.jar app.jar

# Медиа: видео/фото/аудио
COPY media ./media

VOLUME ["/app/data"]

ENTRYPOINT ["java", "-jar", "app.jar"]