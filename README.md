# SGI - Sistema de Gestión de Inventario Multi-Sucursal

[![CI/CD Pipeline](https://github.com/empresa/inventario-sucursales/actions/workflows/ci-cd.yml/badge.svg)](.github/workflows/ci-cd.yml)
[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MySQL 8](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![JaCoCo Coverage](https://img.shields.io/badge/Coverage-%E2%89%A580%25-success.svg)](target/site/jacoco/index.html)

Módulo empresarial robusto para la **gestión centralizada y distribuida de inventarios en 16 sucursales** estratégicas en la República Mexicana. Desarrollado con **Spring Boot 3**, **MySQL 8**, seguridad sin estado con **JSON Web Tokens (JWT)** y control de acceso basado en roles (**RBAC**).

---

## 1. Características Principales

- **16 Sucursales Estratégicas:** CDMX Norte, CDMX Sur, Guadalajara, Monterrey, Puebla, Querétaro, Tijuana, Mérida, Cancún, León, Toluca, Veracruz, Cd. Juárez, San Luis Potosí, Hermosillo y Culiacán.
- **Roles y Permisos Granulares:**
  - **Administrador Corporativo (`ROLE_ADMIN`):**
    - Visibilidad global del inventario en las 16 sucursales.
    - Aprobación o rechazo de solicitudes de traslado o pedidos a proveedor.
    - Actualización directa de stock.
    - Directorio y gestión CRUD completa de empleados por sucursal.
    - Gestión de productos y sucursales.
  - **Gerente de Sucursal (`ROLE_GERENTE`):**
    - Asignado a su sucursal fija.
    - Consulta de existencias locales y de las otras 15 sucursales (para identificar excedentes).
    - Creación de solicitudes de traslado entre sucursales o pedidos a proveedor directo (en estado `PENDIENTE`, sujetas a aprobación del Administrador).
    - **Alertas Visuales de Stock Bajo:** Señalización inmediata en rojo (Agotado / Crítico) y amarillo (Bajo) cuando las unidades caen por debajo del umbral mínimo configurado (`minStockThreshold`).
    - Creación y edición centralizada de productos.
- **Seguridad Robusta:**
  - Autenticación con JWT (HMAC-SHA256).
  - Llave secreta inyectada exclusivamente mediante variable de entorno (`JWT_SECRET`).
  - Bloqueo estricto de peticiones no autenticadas (`401 Unauthorized`) o sin el rol requerido (`403 Forbidden`).
  - Cabeceras de seguridad HTTP configuradas según estándares **OWASP** (`Content-Security-Policy`, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`).
- **Pruebas Automatizadas y Cobertura:**
  - Suite de pruebas con **JUnit 5**, **Mockito** y **Spring Security Test**.
  - Reportes de cobertura con **JaCoCo** superando el objetivo de **80% de cobertura**.
- **DevOps y CI/CD:**
  - Pipeline automatizado de 3 etapas en **GitHub Actions**: Pruebas, Construcción y Despliegue.
  - Despliegue contenerizado con **Docker** y **Docker Compose**.

---

## 2. Matriz de Credenciales de Prueba

Al iniciar la aplicación, la clase `DataInitializer.java` precarga automáticamente las 16 sucursales, productos con stock y usuarios de prueba:

| Usuario | Contraseña | Rol | Sucursal Asignada | Propósito / Alcance |
| :--- | :---: | :---: | :--- | :--- |
| `admin` | `admin123` | `ROLE_ADMIN` | Corporativo (Global) | Acceso total, aprobar solicitudes, CRUD trabajadores |
| `gerente_cdmx` | `gerente123` | `ROLE_GERENTE` | SUC-01 - CDMX Norte | **Dispara alertas visuales de stock bajo** |
| `gerente_mty` | `gerente123` | `ROLE_GERENTE` | SUC-04 - Monterrey Valle | Consulta stock, genera solicitudes de surtido |
| `gerente_gdl` | `gerente123` | `ROLE_GERENTE` | SUC-03 - Guadalajara | Consulta stock multi-sucursal |
| `gerente_puebla`| `gerente123` | `ROLE_GERENTE` | SUC-05 - Puebla | Consulta stock y catálogo |

---

## 3. Requisitos del Sistema

- **Java Development Kit (JDK) 17** o superior.
- **MySQL 8.0** (localmente o mediante contenedor Docker).
- **Maven 3.9+** (o utilizar el script `./mvnw` incluido).
- **Docker Desktop** (opcional, para ejecución con un solo comando o SonarQube).

---

## 4. Instrucciones de Ejecución

### Opción A: Con Docker Compose (Recomendada - Todo en 1 paso)
Inicia la base de datos MySQL 8, la aplicación Spring Boot y SonarQube:
```bash
docker compose up -d
```
- **Aplicación Web:** [http://localhost:8080](http://localhost:8080)
- **SonarQube:** [http://localhost:9000](http://localhost:9000)

---

### Opción B: Ejecución Local con Maven
1. Asegurarse de que el servicio MySQL esté corriendo en el puerto 3306 con base de datos `inventario_db`.
   (O pasar las variables de entorno de conexión correspondientes).
2. Ejecutar la aplicación:
```bash
# En Windows (PowerShell / CMD):
$env:JWT_SECRET="ClaveSecretaSuperSeguraParaFirmarTokensJWTInventarioSucursalesMexico2026DebeTenerMasDe256Bits"
.\mvnw spring-boot:run

# En Linux / macOS:
export JWT_SECRET="ClaveSecretaSuperSeguraParaFirmarTokensJWTInventarioSucursalesMexico2026DebeTenerMasDe256Bits"
./mvnw spring-boot:run
```
3. Abrir el navegador en [http://localhost:8080](http://localhost:8080).

---

## 5. Ejecución de Pruebas Automatizadas y Cobertura (JaCoCo)

Para ejecutar la suite completa de pruebas unitarias y de integración y generar el reporte HTML de JaCoCo:

```bash
# Ejecutar pruebas y generar reporte de cobertura
.\mvnw clean test jacoco:report
```

El reporte detallado de cobertura se generará en:
`target/site/jacoco/index.html`

Para abrir el reporte en el navegador:
```powershell
Start-Process target\site\jacoco\index.html
```

---

## 6. Pipeline de CI/CD (GitHub Actions)

El archivo `.github/workflows/ci-cd.yml` implementa las 3 etapas lógicas obligatorias:
1. **Pruebas (`test`):** Inicia servicio MySQL en contenedor, ejecuta `mvn test jacoco:report`, verifica cobertura $\ge 80\%$ y publica el reporte de JaCoCo como artefacto de la ejecución.
2. **Construcción (`build`):** Compila y empaqueta el artefacto `.jar` de producción y genera la imagen Docker optimizada.
3. **Despliegue (`deploy`):** Despliega automáticamente los contenedores de la aplicación y la base de datos en el entorno de pruebas y valida su disponibilidad mediante el endpoint `/actuator/health`.

---

## 7. Análisis de Calidad y Seguridad

Para consultar las evidencias, métricas y análisis detallados de **SonarQube** y **OWASP ZAP**, así como el **Informe de Cierre** y el **Plan de Mejora Continua**, consulte el documento:
- [`INFORME_CIERRE_Y_MEJORA_CONTINUA.md`](INFORME_CIERRE_Y_MEJORA_CONTINUA.md)
