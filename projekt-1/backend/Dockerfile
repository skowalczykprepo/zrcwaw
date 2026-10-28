# Etap 1: budowanie JAR-a (Maven + JDK 17)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# osobne pobranie zaleznosci -> cache warstwy przy kolejnych buildach
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# Etap 2: lekki obraz uruchomieniowy (samo JRE)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
