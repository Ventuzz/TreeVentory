# Etapa 1: Construcción del artefacto
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copiar archivos del proyecto
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw

# Descargar dependencias para cache de capas
RUN ./mvnw dependency:go-offline -B || true

# Copiar código fuente y compilar
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Etapa 2: Imagen de ejecución ligera
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Crear usuario no privilegiado para seguridad
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

# Copiar el artefacto generado
COPY --from=build /app/target/*.jar app.jar

# Variables de entorno con valores por defecto
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod
ENV JWT_SECRET=ClaveSecretaSuperSeguraParaFirmarTokensJWTInventarioSucursalesMexico2026DebeTenerMasDe256Bits

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
