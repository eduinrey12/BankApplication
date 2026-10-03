# BankApplication - Sistema de Microservicios Bancarios

Sistema backend para la gestión de clientes, cuentas bancarias, transacciones financieras y emisión de estados de cuenta consolidados. Desarrollado con **Java 11** y **Spring Boot**, siguiendo una arquitectura de microservicios desacoplada, consistencia transaccional ACID y prácticas de seguridad bancaria.

[![Java](https://img.shields.io/badge/Java-11-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Enabled-blue.svg)](https://www.docker.com/)
[![Railway](https://img.shields.io/badge/Railway-Live%20Cloud-success.svg)](https://railway.app/)
[![Tests](https://img.shields.io/badge/Tests-Passing-brightgreen.svg)]()

---

## Despliegue en la Nube (Railway Cloud)

Ambos microservicios se encuentran desplegados, configurados y comunicándose entre sí vía HTTPS en Railway:

* **Client Service Base URL:** https://bankapplication-production-c454.up.railway.app
* **Account Service Base URL:** https://account-service-production-077e.up.railway.app
* **Health Check & Sondas Actuator:** https://account-service-production-077e.up.railway.app/actuator/health

### Endpoints activos listos para prueba directa en producción:
* **Listar Clientes:** [`GET /api/clients`](https://bankapplication-production-c454.up.railway.app/api/clients)
* **Listar Cuentas:** [`GET /api/accounts`](https://account-service-production-077e.up.railway.app/api/accounts)
* **Listar Transacciones:** [`GET /api/transactions`](https://account-service-production-077e.up.railway.app/api/transactions)
* **Estado de Cuenta Consolidado (F4):** [`GET /api/transactions/clients/1/report?dateTransactionStart=2026-01-01&dateTransactionEnd=2026-12-31`](https://account-service-production-077e.up.railway.app/api/transactions/clients/1/report?dateTransactionStart=2026-01-01&dateTransactionEnd=2026-12-31)

---

## Matriz de Cumplimiento (Especificación del Examen)

| Requerimiento | Especificación del PDF | Implementación en Código | Estado |
| :--- | :--- | :--- | :---: |
| **Separación de Servicios** | 2 microservicios independientes (puertos 8000 y 8001) | `account-service` (8000) y `client-service` (8001) con almacenamiento independiente. | Cumplido |
| **Modelo de Dominio** | Entidad `Cliente` hereda de `Persona` | `Client extends Person` con `@MappedSuperclass` y atributos completos del PDF. | Cumplido |
| **F1: CRUD de Entidades** | GET, POST, PUT, PATCH, DELETE en Clientes, Cuentas y Movimientos | Endpoints REST en ambos servicios con códigos HTTP formales (200, 201, 204). | Cumplido |
| **F2: Registro de Movimientos** | Débitos (-) y Créditos (+) con cálculo de saldo | `TransactionServiceImpl.create()` calcula el saldo en tiempo real con `BigDecimal`. | Cumplido |
| **F3: Saldo No Disponible** | Si el saldo es insuficiente para un débito, rechazar | Lanza `InsufficientFundsException ("Saldo no disponible")` retornando HTTP 400 Bad Request. | Cumplido |
| **F4: Estado de Cuenta** | Reporte detallado por rango de fechas y cliente | Endpoint `/api/transactions/clients/{clientId}/report` con formato JSON solicitado. | Cumplido |
| **F4.1.1: Cuentas sin Movimientos** | Incluir cuentas del cliente aunque no registren movimientos | Consulta agrupada que lista todas las cuentas asociadas con saldo actual y etiqueta informativa. | Cumplido |
| **F5: Pruebas Unitarias** | Pruebas unitarias de dominio para la entidad Cliente | `sampleTest.java` en `client-service` validando instanciación, herencia y atributos. | Cumplido |
| **F6: Pruebas de Integración** | Pruebas de integración con base de datos H2 | `sampleTest.java` en ambos servicios persistiendo flujos de negocio completos. | Cumplido |
| **Control de Concurrencia** | Prevenir condiciones de carrera en débitos simultáneos | Bloqueo pesimista `@Lock(PESSIMISTIC_WRITE)` verificado con test multihilo (`CountDownLatch`). | Cumplido |
| **Contenedores Docker** | Dockerfiles y orquestación con Docker Compose | Multi-stage build con `eclipse-temurin:11-jre` y `docker-compose.yml` en red interna. | Cumplido |
| **Colección de Postman** | Archivo JSON con casos de prueba listos para importar | Archivo `collection_bank_postman.json` en la raíz del repositorio. | Cumplido |

---

## Arquitectura del Sistema

```
                  +-----------------------------------+
                  |         Cliente / Postman         |
                  +-----------------------------------+
                            |               |
             HTTP (Puerto 8001)             | HTTP (Puerto 8000)
                            v               v
            +--------------------+     +---------------------+
            |   client-service   |     |   account-service   |
            |                    |     |                     |
            | - Person (Base)    |     | - Cuentas           |
            | - Clientes         | <---| - Transacciones     |
            | - Base H2 / JPA    |REST | - Reportes (F4)     |
            | - Actuator / Salud |Async| - Base H2 / JPA     |
            +--------------------+     +---------------------+
```

### Componentes:
- **`client-service` (Puerto 8001):** Gestiona los datos demográficos y de acceso de clientes bancarios (`Person` y `Client`).
- **`account-service` (Puerto 8000):** Administra cuentas bancarias, procesa transacciones financieras con validación de saldo disponible y genera estados de cuenta consolidados. Para asociar el nombre del cliente en el reporte F4, consulta a `client-service` de forma asíncrona mediante `CompletableFuture`.

---

## Decisiones de Diseño e Ingeniería

### 1. Control de Concurrencia (Bloqueo Pesimista)
En un entorno bancario, débitos simultáneos sobre la misma cuenta (por ejemplo, dos retiros concurrentes de $80 sobre un saldo de $100) deben serializarse para evitar sobregiro (*double-spending*):
- Se implementó `@Lock(LockModeType.PESSIMISTIC_WRITE)` a nivel de consulta JPA (`SELECT ... FOR UPDATE`) sobre la cuenta.
- La operación corre bajo transacción `@Transactional(isolation = Isolation.READ_COMMITTED)`. El primer hilo descuenta los fondos y confirma el nuevo saldo ($20). Cuando el segundo hilo obtiene el bloqueo, lee el saldo actualizado, comprueba que $80 supera los $20 disponibles y rechaza la transacción con HTTP 400 ("Saldo no disponible").

### 2. Aritmética Financiera con `BigDecimal`
- Los tipos primitivos de coma flotante (`double` o `float`) sufren de imprecisión binaria bajo la norma IEEE-754.
- Todo el cálculo de saldos y movimientos se realiza internamente con `BigDecimal`, aplicando redondeo financiero bancario (`RoundingMode.HALF_EVEN`) a dos decimales.

### 3. Optimización de Consultas (Anti-N+1)
- En el reporte consolidado de estado de cuenta (F4), un cliente puede poseer múltiples cuentas y cientos de transacciones.
- Para evitar ejecutar una consulta por cada cuenta (problema de consulta N+1), se utiliza una única consulta agrupada `findByAccountIdInAndDateBetween`, recuperando todos los movimientos del periodo en un solo viaje a la base de datos.
- Se configuraron índices en las entidades: `(account_id, date)` en `Transaction`, `(client_id)` en `Account` y `(dni)` en `Client`.

### 4. Resiliencia HTTP Inter-servicio
- La comunicación entre `account-service` y `client-service` utiliza un `RestTemplate` configurado con timeouts estrictos:
  - **Connect Timeout:** 3.000 ms
  - **Read Timeout:** 5.000 ms
- Esto previene el agotamiento del pool de hilos de Tomcat (*thread starvation*) en caso de degradación o lentitud en el servicio remoto.
- HikariCP se encuentra ajustado con un pool máximo de 30 conexiones y detección de fugas en 2.000 ms.

### 5. Seguridad y Validación de Datos
- **Protección de Contraseñas:** Se anotó `@JsonProperty(access = Access.WRITE_ONLY)` en el DTO de clientes para recibir la contraseña en altas y modificaciones, pero omitirla en las respuestas JSON de lectura.
- **Validación de Payloads (JSR-380):** Validaciones declarativas (`@NotBlank`, `@NotNull`, `@Valid`) en DTOs y controladores, capturadas por `GlobalExceptionHandler` para devolver respuestas HTTP 400 detalladas por campo.
- **Cabeceras HTTP de Seguridad:** Filtro perimetral `SecurityHeadersFilter` activo en ambos servicios inyectando `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `X-XSS-Protection: 1; mode=block` y `Referrer-Policy`.
- **Manejo Centralizado de Excepciones:** Errores no controlados generan un identificador único de incidente (`UUID incidentId`) para trazabilidad en logs del servidor, evitando exponer trazas de depuración o detalles internos al exterior.

### 6. Observabilidad (Spring Boot Actuator)
- Ambos servicios exponen `/actuator/health` con sondas nativas `liveness` y `readiness`, listas para ser integradas con orquestadores de contenedores (Kubernetes / OpenShift / Docker).

---

## Guía de Uso Rápido (Ejemplos cURL / JSON)

A continuación se presentan los ejemplos directos para interactuar con la API (localmente en los puertos indicados o sustituyendo por las URLs de Railway):

### 1. Registrar un Cliente
```bash
curl -X POST http://localhost:8001/api/clients \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "1712345678",
    "name": "Jose Lema",
    "password": "Password123",
    "gender": "Masculino",
    "age": 35,
    "address": "Otavalo sn y principal",
    "phone": "098254785",
    "isActive": true
  }'
```

### 2. Abrir una Cuenta Bancaria
```bash
curl -X POST http://localhost:8000/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "number": "478758",
    "type": "Ahorro",
    "initialAmount": 2000.0,
    "isActive": true,
    "clientId": 1
  }'
```

### 3. Registrar Transacciones
```bash
# Depósito (+575.00)
curl -X POST http://localhost:8000/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": 1,
    "type": "Deposito",
    "amount": 575.0
  }'

# Retiro (-540.00)
curl -X POST http://localhost:8000/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": 1,
    "type": "Retiro",
    "amount": -540.0
  }'
```

### 4. Prueba de Saldo Insuficiente (F3)
Al solicitar un retiro que exceda el saldo disponible:
```bash
curl -X POST http://localhost:8000/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": 1,
    "type": "Retiro",
    "amount": -50000.0
  }'
```
**Respuesta devuelta (HTTP 400 Bad Request):**
```json
{
  "message": "Saldo no disponible"
}
```

### 5. Generar Estado de Cuenta Consolidado (F4)
```bash
curl -X GET "http://localhost:8000/api/transactions/clients/1/report?dateTransactionStart=2026-01-01&dateTransactionEnd=2026-12-31"
```

---

## Catálogo de Endpoints REST

### Client Service (`client-service` - Puerto 8001)

| Método | Endpoint | Descripción | Código Éxito |
| :--- | :--- | :--- | :---: |
| `GET` | `/api/clients` | Listar todos los clientes registrados | `200 OK` |
| `GET` | `/api/clients/{id}` | Consultar detalle de un cliente por su ID | `200 OK` |
| `POST` | `/api/clients` | Registrar un nuevo cliente | `201 Created` |
| `PUT` | `/api/clients/{id}` | Actualización completa de datos del cliente | `200 OK` |
| `PATCH`| `/api/clients/{id}` | Actualización parcial de estado (`isActive`) | `200 OK` |
| `DELETE`| `/api/clients/{id}` | Eliminación de cliente por ID | `200 OK` |
| `GET` | `/actuator/health` | Sonda de salud y estado del servicio | `200 OK` |

### Account Service (`account-service` - Puerto 8000)

| Método | Endpoint | Descripción | Código Éxito |
| :--- | :--- | :--- | :---: |
| `GET` | `/api/accounts` | Listar todas las cuentas bancarias | `200 OK` |
| `GET` | `/api/accounts/{id}` | Consultar cuenta por ID | `200 OK` |
| `POST` | `/api/accounts` | Apertura de cuenta corriente o de ahorros | `201 Created` |
| `PUT` | `/api/accounts/{id}` | Actualizar datos de cuenta | `200 OK` |
| `PATCH`| `/api/accounts/{id}` | Actualización parcial de estado de cuenta | `200 OK` |
| `DELETE`| `/api/accounts/{id}` | Eliminar cuenta | `200 OK` |
| `GET` | `/api/transactions` | Listar histórico de transacciones | `200 OK` |
| `GET` | `/api/transactions/{id}` | Consultar transacción por ID | `200 OK` |
| `POST` | `/api/transactions` | Registrar depósito/retiro (con validación de saldo) | `201 Created` |
| `PUT` | `/api/transactions/{id}` | Actualizar datos de transacción | `200 OK` |
| `PATCH`| `/api/transactions/{id}` | Actualización parcial de transacción | `200 OK` |
| `DELETE`| `/api/transactions/{id}`| Eliminar registro de transacción | `200 OK` |
| `GET` | `/api/transactions/clients/{clientId}/report` | **F4: Extracto Bancario Consolidado** (`?dateTransactionStart=YYYY-MM-DD&dateTransactionEnd=YYYY-MM-DD`) | `200 OK` |
| `GET` | `/actuator/health` | Sonda de salud y preparación del servicio | `200 OK` |

---

## Pruebas y Validación de Calidad

El proyecto cuenta con validación en dos niveles: pruebas automatizadas en código fuente (JUnit 5 / Spring Test) y auditoría de carga, estrés y concurrencia ejecutada sobre el entorno de producción desplegado en Railway Cloud.

### 1. Pruebas Automatizadas en Código (JUnit 5)

Ubicadas en `sampleTest.java` de cada microservicio:

* **F5 (Prueba Unitaria de Dominio):** Verifica la entidad `Client` y la correcta herencia de atributos de `Person`.
* **F3 (Prueba de Saldo Insuficiente):** Valida que débitos mayores al saldo disponible lancen `InsufficientFundsException ("Saldo no disponible")` retornando HTTP 400.
* **F6 (Pruebas de Integración End-to-End):** Pruebas integradas con persistencia H2 validando el ciclo de vida completo de clientes, cuentas y transacciones.
* **Prueba Multihilo de Concurrencia (`concurrentTransactionsRaceConditionTest`):** Dos hilos concurrentes compiten con `CountDownLatch` y `ExecutorService` por un retiro de $80 sobre un saldo de $100. Verifica que el bloqueo pesimista aprueba exactamente una transacción, rechaza la segunda y mantiene el balance final en $20 sin posibilidad de sobregiro.

```bash
# Pruebas en client-service
cd client
./mvnw test

# Pruebas en account-service
cd ../account
./mvnw test
```

### 2. Pruebas de Carga, Estrés y Concurrencia en Producción (Cloud Benchmark)

Se ejecutó una batería de pruebas de carga masiva y estrés concurrente contra los microservicios desplegados en **Railway Cloud (HTTPS)** utilizando sesiones HTTP multiplexadas:

* **Ataque de Concurrencia Extrema (50 hilos simultáneos):**
  Se lanzó una barrera sincronizada de 50 peticiones simultáneas intentando debitar $80 cada una sobre una cuenta con saldo inicial de $100.
  - **Resultado:** Exactamente 1 transacción aprobada (HTTP 201), 49 rechazadas con "Saldo no disponible" (HTTP 400).
  - **Saldo Final:** $20.00 exactos. Sobregiros detectados: 0. Condición de carrera mitigada al 100%.

* **Rendimiento Bajo Carga Masiva:**
  Se enviaron ráfagas continuas de peticiones concurrentes para medir throughput y tiempos de respuesta reales a través de internet:

| Métrica / Prueba | Entorno Local (H2 / Localhost) | Entorno Producción (Railway Cloud) |
| :--- | :---: | :---: |
| **Throughput (Rendimiento)** | 741.34 req/seg | 265.69 req/seg |
| **Latencia Mediana (p50)** | 29.83 ms | 97.76 ms |
| **Tasa de Errores HTTP** | 0.00% | 0.00% |
| **Consistencia Transaccional (50 hilos)** | 100% (0 saldos negativos) | 100% (0 saldos negativos) |
| **Sondas Actuator / Health** | UP | UP |
| **Cabeceras de Seguridad OWASP** | Verificadas | Verificadas en HTTPS |

* **Seguridad Defensiva Verificada en Vivo:**
  - Inyección de cabeceras HTTP de seguridad (`X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `X-XSS-Protection`).
  - Ocultamiento de contraseñas (`@JsonProperty(access = WRITE_ONLY)`).
  - Trazabilidad de errores mediante `UUID incidentId` en respuestas HTTP 500 sin exponer stacktraces.

---

## Ejecución Local

### Opción 1: Docker Compose (Recomendado)
Compila y levanta ambos microservicios en contenedores aislados mediante Multi-Stage Build con `eclipse-temurin:11-jre`:

```bash
docker-compose up --build
```

### Opción 2: Maven Wrapper Local
Requiere Java 11 o superior instalado:

```bash
# Terminal 1: Client Service (Puerto 8001)
cd client
./mvnw spring-boot:run

# Terminal 2: Account Service (Puerto 8000)
cd account
./mvnw spring-boot:run
```

---

## Colección de Postman

Se incluye el archivo listo para importar en la raíz del proyecto:
* **Archivo:** `collection_bank_postman.json`
* **Casos cubiertos:** Creación de clientes (`Jose Lema`, `Marianela Montalvo`), apertura de cuentas (Ahorro y Corriente), depósitos, retiros válidos, validación de saldo insuficiente (HTTP 400) y generación del estado de cuenta consolidado con filtros de fecha (F4).

---

## Autor

**Edwin Rey**  
Software Engineer | Backend & Cloud Architecture  
