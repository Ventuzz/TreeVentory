# SGI - Sistema de Gestión de Inventario Multi-Sucursal (Treeventory)

[![CI/CD Pipeline](https://img.shields.io/badge/CI%2FCD-Passing-brightgreen.svg)](.github/workflows/ci-cd.yml)
[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MySQL 8](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![JaCoCo Coverage](https://img.shields.io/badge/Coverage-89%25-brightgreen.svg)](target/site/jacoco/index.html)
[![Docker](https://img.shields.io/badge/Docker-Enabled-blue.svg)](https://www.docker.com/)

Plataforma empresarial de grado de producción diseñada para la **administración centralizada, trazabilidad y control de inventarios en tiempo real a través de 16 sucursales estratégicas** en la República Mexicana. El sistema implementa una arquitectura en capas desacoplada con **Spring Boot 3**, persistencia relacional transaccional en **MySQL 8**, seguridad sin estado mediante **JSON Web Tokens (JWT)** y control de acceso basado en roles (**RBAC**).

---

## Objetivo del Proyecto

El **Sistema de Gestión de Inventario (SGI)** resuelve la problemática de sincronización, desabasto y falta de visibilidad en cadenas comerciales distribuidas geográficamente. Sus propósitos fundamentales son:

* **Control Multi-Sucursal en Tiempo Real:** Gestionar de forma atómica y consistente las existencias de productos en 16 sucursales clave (CDMX Norte, CDMX Sur, Guadalajara, Monterrey, Puebla, Querétaro, Tijuana, Mérida, Cancún, León, Toluca, Veracruz, Cd. Juárez, San Luis Potosí, Hermosillo y Culiacán).
* **Control de Acceso Basado en Roles (RBAC):**
  * **Administrador Corporativo (`ROLE_ADMIN`):** Posee supervisión global sobre todas las sucursales, autoriza o rechaza solicitudes de reabastecimiento o traslado entre sucursales, administra el catálogo maestro de productos y gestiona el directorio de personal (CRUD de empleados).
  * **Gerente de Sucursal (`ROLE_GERENTE`):** Supervisa el inventario de su sucursal fija, consulta existencias de otras sucursales para detectar excedentes y emite solicitudes de traspaso de mercancía o pedidos a proveedores directos.
* **Alertas Visuales de Desabasto Automáticas:** Notificación inmediata mediante un semáforo visual de criticidad (Rojo = Agotado/Crítico, Amarillo = Stock Bajo) cuando las existencias caen por debajo del umbral mínimo configurado (`minStockThreshold`), evitando quiebres de inventario.
* **Trazabilidad y Calidad de Grado Empresarial:** Asegurar la integridad de las transacciones (ACID), cobertura de pruebas automatizadas y tuberías continuas de integración y despliegue (**CI/CD**).

---

## Instalación de Dependencias y Requisitos Previos

### Requisitos del Sistema
Antes de comenzar, asegúrese de contar con el siguiente software instalado:

* **Java Development Kit (JDK) 17** o superior (ej. [Eclipse Temurin](https://adoptium.net/) u OpenJDK).
  * Comprobar versión instalada:
    ```bash
    java -version
    ```
* **Docker y Docker Desktop** (Recomendado para levantar MySQL y SonarQube sin instalaciones locales complejas).
  * Comprobar versión instalada:
    ```bash
    docker --version
    docker compose version
    ```
* **Git** para clonar y gestionar el repositorio.
* **Maven 3.9+** (Opcional, el repositorio incluye los wrappers ejecutables `mvnw` y `mvnw.cmd` que no requieren instalación manual de Maven).

### Instalación / Descarga de Dependencias del Proyecto
El proyecto utiliza **Maven** para la resolución y descarga automática de librerías desde los repositorios centrales. 

Para forzar la descarga e instalación de todas las dependencias declaradas en `pom.xml` (`spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-data-jpa`, `jjwt`, `mysql-connector-j`, `h2`, `jacoco`, etc.):

#### En Windows (PowerShell / CMD):
```powershell
# Descarga e inspección de dependencias
.\mvnw.cmd clean dependency:resolve

# Compilar el proyecto y descargar librerías faltantes
.\mvnw.cmd clean compile
```

#### En Linux / macOS:
```bash
# Otorgar permisos de ejecución al wrapper
chmod +x mvnw

# Descarga y resolución de dependencias
./mvnw clean dependency:resolve

# Compilar el proyecto
./mvnw clean compile
```

---

## Configuración de Variables de Entorno

La aplicación está parametrizada mediante el archivo `src/main/resources/application.properties` con valores por defecto y soporte para **sobreescritura transparente mediante variables de entorno**:

| Variable de Entorno | Valor por Defecto / Ejemplo | Descripción |
| :--- | :--- | :--- |
| `PORT` | `8080` | Puerto TCP en el que escucha la aplicación web. |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/inventario_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true` | Cadena de conexión JDBC hacia la base de datos MySQL. |
| `SPRING_DATASOURCE_USERNAME` | `root` | Usuario para la autenticación en la base de datos. |
| `SPRING_DATASOURCE_PASSWORD` | `root` (en Docker) / `admin` (en local) | Contraseña de la base de datos. |
| `JWT_SECRET` | *(Clave secreta segura de 256 bits)* | Llave criptográfica simétrica HMAC-SHA256 para firmar y validar tokens de sesión. |
| `JWT_EXPIRATION_MS` | `86400000` (24 horas en ms) | Duración de la validez del token JWT generado. |

### Cómo configurar las variables de entorno en su terminal:

#### En Windows (PowerShell):
```powershell
$env:PORT="8080"
$env:JWT_SECRET="ClaveSecretaSuperSeguraParaFirmarTokensJWTInventarioSucursalesMexico2026DebeTenerMasDe256Bits"
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/inventario_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:SPRING_DATASOURCE_USERNAME="root"
$env:SPRING_DATASOURCE_PASSWORD="root"
```

#### En Windows (Símbolo del Sistema / CMD):
```cmd
set PORT=8080
set JWT_SECRET=ClaveSecretaSuperSeguraParaFirmarTokensJWTInventarioSucursalesMexico2026DebeTenerMasDe256Bits
set SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/inventario_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
set SPRING_DATASOURCE_USERNAME=root
set SPRING_DATASOURCE_PASSWORD=root
```

#### En Linux / macOS:
```bash
export PORT="8080"
export JWT_SECRET="ClaveSecretaSuperSeguraParaFirmarTokensJWTInventarioSucursalesMexico2026DebeTenerMasDe256Bits"
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/inventario_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
export SPRING_DATASOURCE_USERNAME="root"
export SPRING_DATASOURCE_PASSWORD="root"
```

> **Nota de Seguridad:** En entornos de producción y en GitHub Actions, estas variables deben configurarse como secretos protegidos (`GitHub Secrets`) o variables de entorno del servidor, evitando exponer credenciales en el código fuente.

---

## Ejecución de la Aplicación

Existen dos modalidades para poner en marcha el sistema:

---

### Opción A: Despliegue con Docker Compose
Levanta de forma automática el contenedor de MySQL 8.0 y el contenedor de la aplicación Spring Boot interconectados en una red privada virtual.

1. **Iniciar los servicios en segundo plano:**
   ```bash
   docker compose up -d db app
   ```
2. **Verificar el estado de los contenedores:**
   ```bash
   docker compose ps
   ```
   *Deberá ver tanto `inventario-mysql` como `inventario-app` en estado `Up` o `healthy`.*

3. **Ver los logs de inicio de la aplicación:**
   ```bash
   docker compose logs -f app
   ```

4. **Acceder al sistema:**
   * Abra su navegador web en: **[http://localhost:8080](http://localhost:8080)**

5. **Para detener los servicios:**
   ```bash
   docker compose down
   ```
   *(Si desea borrar también los datos almacenados en los volúmenes persistentes, use: `docker compose down -v`)*.

---

### Ejecución Local con Maven
Si prefiere ejecutar la aplicación directamente en su JVM de desarrollo:

1. **Iniciar la base de datos MySQL:**
   Puede utilizar el servicio de base de datos de Docker Compose:
   ```bash
   docker compose up -d db
   ```
   *O bien tener una instancia de MySQL local corriendo en el puerto 3306 con la base `inventario_db`.*

2. **Arrancar la aplicación Spring Boot:**
   * **En Windows (PowerShell):**
     ```powershell
     $env:JWT_SECRET="ClaveSecretaSuperSeguraParaFirmarTokensJWTInventarioSucursalesMexico2026DebeTenerMasDe256Bits"
     .\mvnw.cmd spring-boot:run
     ```
   * **En Linux / macOS:**
     ```bash
     export JWT_SECRET="ClaveSecretaSuperSeguraParaFirmarTokensJWTInventarioSucursalesMexico2026DebeTenerMasDe256Bits"
     ./mvnw spring-boot:run
     ```

3. **Acceder a la aplicación:**
   * Diríjase a: **[http://localhost:8080](http://localhost:8080)**

---

## Ejecución de Pruebas Automatizadas

La arquitectura del proyecto incluye pruebas automatizadas en dos niveles:
* **Pruebas de Servicios (Lógica de Negocio):** Pruebas unitarias con JUnit 5 y Mockito para validar reglas de negocio, stock, transferencias y transacciones.
* **Pruebas de Controladores (Capa Web y Seguridad RBAC):** Pruebas de integración con `MockMvc` para validar códigos HTTP (`200`, `201`, `401`, `403`), serialización JSON, DTOs y filtros de seguridad.
* **Aislamiento:** Las pruebas utilizan una base de datos en memoria **H2** de alta velocidad, por lo que **no requieren que MySQL esté encendido para ejecutarse**.

### Ejecutar toda la suite de pruebas
```powershell
# En Windows:
.\mvnw.cmd test

# En Linux / macOS:
./mvnw test
```

### Ejecutar una clase o prueba específica
```powershell
# Probar únicamente el servicio de solicitudes de inventario:
.\mvnw.cmd test -Dtest=RequestServiceTest

# Probar la seguridad de endpoints y roles RBAC:
.\mvnw.cmd test -Dtest=SecurityAndControllerIntegrationTest

# Probar la cobertura de los controladores REST:
.\mvnw.cmd test -Dtest=ControllerCoverageTest
```

### Ejecutar pruebas con validación del umbral JaCoCo ($\ge 80\%$)
Para verificar que el código cumple con el estándar de calidad y no rompe las reglas del pipeline:
```powershell
.\mvnw.cmd clean verify
```
*(Si la cobertura total de líneas cae por debajo del 80%, Maven fallará el build informando la violación de la regla).*

---

## Ejecución y Consulta de Análisis de Calidad y Seguridad

---

### Cobertura de Código con JaCoCo
**JaCoCo (Java Code Coverage)** analiza el *bytecode* ejecutado durante las pruebas y cuantifica el porcentaje de instrucciones, ramas (`if/else`) y líneas cubiertas.

1. **Generar el reporte de JaCoCo:**
   ```powershell
   # En Windows:
   .\mvnw.cmd clean test jacoco:report

   # En Linux / macOS:
   ./mvnw clean test jacoco:report
   ```

2. **Ubicación del reporte generado:**
   El reporte interactivo en formato HTML se crea en:
   ```text
   target/site/jacoco/index.html
   ```

3. **Consultar el reporte en el navegador:**
   * **En Windows (PowerShell):**
     ```powershell
     Start-Process target\site\jacoco\index.html
     ```
   * **En Linux / macOS:**
     ```bash
     xdg-open target/site/jacoco/index.html || open target/site/jacoco/index.html
     ```

4. **Resultados de Cobertura Obtenidos en el Proyecto:**
   * **Cobertura Global de Instrucciones:** **89%** (1,491 de 1,665 instrucciones ejecutadas).
   * **Cobertura de Líneas de Código:** **96%** (351 de 367 líneas cubiertas).
   * **Paquete `controller` (Controladores REST):** **98%** de cobertura de instrucciones y **100%** de líneas probadas.
   * **Paquete `service` (Reglas de Negocio):** **86%** de cobertura general.


---

### Análisis Estático de Calidad y Seguridad con SonarQube
SonarQube permite detectar deuda técnica, bugs potenciales, vulnerabilidades de seguridad (*Security Hotspots*) y *code smells*.

1. **Iniciar el servidor local de SonarQube:**
   ```bash
   docker compose up -d sonarqube
   ```
2. **Acceder a la consola web de SonarQube:**
   * Ingrese a: **[http://localhost:9000](http://localhost:9000)**
   * Credenciales por defecto: Usuario `admin`, Contraseña `admin`.
   * *(Opcional: Genere un token de análisis en Perfil > Security > Generate Token)*.

3. **Ejecutar el escaneo con Maven integrado a JaCoCo:**
   ```powershell
   # En Windows:
   .\mvnw.cmd clean verify sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=TU_TOKEN_AQUI

   # En Linux / macOS:
   ./mvnw clean verify sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=TU_TOKEN_AQUI
   ```
4. **Consultar resultados:**
   Actualice la página en `http://localhost:9000` para ver la matriz de calidad, calificación de seguridad (**Rating A**), duplicación de código y cobertura importada de JaCoCo.

---

### Verificación de Salud y Seguridad en Tiempo de Ejecución (Actuator)
La aplicación cuenta con endpoints de diagnóstico para evaluar la operatividad del sistema:

* **Health Check (Disponibilidad y conexión a base de datos):**
  ```bash
  curl -i http://localhost:8080/actuator/health
  ```
  *Respuesta esperada:*
  ```json
  {"status":"UP","components":{"db":{"status":"UP"},"diskSpace":{"status":"UP"}}}
  ```
* **Cabeceras HTTP de Seguridad (OWASP):**
  La aplicación inyecta cabeceras de protección activa contra ataques comunes (Clickjacking, XSS, MIME sniffing):
  * `X-Frame-Options: DENY`
  * `X-Content-Type-Options: nosniff`
  * `Content-Security-Policy: default-src 'self' ...`

---

### Pipeline de Integración y Despliegue Continuo (CI/CD en GitHub Actions)
El flujo automatizado definido en [`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml) se dispara en cada `push` o `pull request` a las ramas principales (`main` / `master`) y consta de 3 etapas secuenciales:

1. **Etapa 1 - Pruebas (`test`):**
   * Configura entorno Java 17 Temurin.
   * Ejecuta `./mvnw clean verify` (Pruebas unitarias e integrales).
   * Publica el reporte de JaCoCo como artefacto descargable en GitHub (`jacoco-coverage-report`).
2. **Etapa 2 - Construcción (`build`):**
   * Empaqueta el archivo binario ejecutable `.jar` de producción.
   * Construye y valida la imagen Docker multicapa optimizada.
   * Publica el `.jar` como artefacto de la ejecución (`inventario-sucursales-app-jar`).
3. **Etapa 3 - Despliegue (`deploy`):**
   * Despliega la base de datos MySQL y la aplicación en un entorno de pruebas con Docker Compose.
   * Realiza un *Smoke Test* automatizado sondeando `/actuator/health` hasta obtener un código `200 OK`.

---

## Matriz de Credenciales de Prueba

Al iniciar el sistema, el componente `DataInitializer` precarga automáticamente el catálogo maestro de 16 sucursales, productos iniciales y las siguientes cuentas de prueba:

| Usuario | Contraseña | Rol | Sucursal Asignada | Alcance / Permisos |
| :--- | :---: | :---: | :--- | :--- |
| `admin` | `admin123` | `ROLE_ADMIN` | Corporativo (Global) | **Acceso Total:** Aprobar/rechazar solicitudes, actualizar stock, CRUD completo de empleados y catálogos. |
| `gerente_cdmx` | `gerente123` | `ROLE_GERENTE` | SUC-01 - CDMX Norte | **Dispara alertas visuales de stock bajo/agotado.** Crea solicitudes de traspaso y surtido. |
| `gerente_mty` | `gerente123` | `ROLE_GERENTE` | SUC-04 - Monterrey Valle | Consulta stock local y multi-sucursal, genera solicitudes. |
| `gerente_gdl` | `gerente123` | `ROLE_GERENTE` | SUC-03 - Guadalajara | Consulta stock multi-sucursal y gestión local. |
| `gerente_puebla` | `gerente123` | `ROLE_GERENTE` | SUC-05 - Puebla | Consulta stock local y catálogo. |
