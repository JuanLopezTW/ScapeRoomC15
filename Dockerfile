# --- Etapa 1: compilar todo el reactor y generar el jar ejecutable de app ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
# El cache de ~/.m2 evita bajar todas las dependencias en cada build
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -DskipTests package

# --- Etapa 2: imagen liviana solo con el JRE y el jar ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/app/target/app-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
