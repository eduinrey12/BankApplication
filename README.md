# BankApplication - Sistema de Microservicios Bancarios

Solución técnica para el sistema bancario desarrollada con **Spring Boot**, basada en una arquitectura de microservicios desacoplada, escalable y resiliente para la gestión de clientes, cuentas, transacciones y emisión de estados de cuenta consolidados.

---

## 🏛️ Arquitectura de la Solución

El sistema se encuentra dividido en dos microservicios autónomos, siguiendo los principios de alta cohesión y bajo acoplamiento:

```
                      +---------------------------------------+
                      |               Cliente                 |
                      |        (Postman / Web / App)          |
                      +---------------------------------------+
                                  |                 |
                   (Port 8001)    |                 | (Port 8000)
                                  v                 v
            +-------------------------+         +--------------------------+
            |      client-service     |         |     account-service      |
            |                         |         |                          |
            |  - Person (Base Model)  |         |  - Account Management    |
            |  - Client Management    | <.......|  - Transaction Registry  |
            |  - H2 Database (JPA)    |  REST/  |  - Bank Statement Report |
            +-------------------------+  Async  |  - H2 Database (JPA)     |
                                                +--------------------------+
```

### 1. `client-service` (Puerto 8001)
* **Entidades**: `Person` (modelo base con `@MappedSuperclass`) y `Client` (entidad JPA con herencia).
* **Responsabilidad**: Gestión del ciclo de vida de clientes bancarios (creación, consulta, actualización total/parcial y baja lógica/física).

### 2. `account-service` (Puerto 8000)
* **Entidades**: `Account` (cuentas corrientes y de ahorros) y `Transaction` (movimientos financieros con balance dinámico).
* **Responsabilidad**: 
  - Gestión integral de cuentas bancarias asociadas a clientes.
  - Procesamiento transaccional de depósitos (positivos) y retiros (negativos) con validación estricta de saldo disponible.
  - Generación de reportes de estado de cuenta por rango de fechas y cliente consolidado.

---

## 🚀 Principios de Diseño y Buenas Prácticas Senior

* **Arquitectura en Capas Limpia**: Separación estricta entre capa de presentación (`Controller`), lógica de negocio (`Service`), persistencia (`Repository`) y transferencia de datos (`DTO`).
* **Inmutabilidad y DTO Pattern**: Desacoplamiento de las entidades JPA de la capa web mediante `ClientDto`, `AccountDto`, `TransactionDto` y `BankStatementDto`.
* **Control Centralizado de Excepciones**: Uso de `@RestControllerAdvice` y `@ExceptionHandler` para retornar respuestas HTTP estandarizadas. En caso de fondos insuficientes, se captura la regla de negocio y se emite la alerta **"Saldo no disponible"** con código `400 Bad Request`.
* **Trazabilidad de Saldo Histórico**: Cada transacción registra el monto aplicado y el balance resultante en tiempo real, garantizando auditoría contable.
* **Resiliencia e Integración**: `account-service` se comunica con `client-service` para obtener los datos nominativos del cliente en el reporte. Incluye mecanismos de tolerancia a fallos con fallback para mantener la disponibilidad del servicio.

### 💡 Consideraciones de Escalabilidad, Rendimiento y Asincronía
En un entorno productivo de alta concurrencia:
* **Event-Driven Architecture (EDA)**: Los microservicios pueden comunicarse asíncronamente mediante **Apache Kafka** o **RabbitMQ**. Al registrar un cliente o movimiento, se emiten eventos de dominio (`ClientRegisteredEvent`, `TransactionCompletedEvent`).
* **Transactional Outbox Pattern**: Garantiza consistencia eventual entre el guardado en base de datos y la publicación de eventos sin transacciones distribuidas bloqueantes (2PC).
* **Idempotencia y Bloqueo Optimista**: Aplicación de `@Version` en cuentas para prevenir condiciones de carrera en transacciones concurrentes sobre la misma cuenta.

---

## 📡 Catálogo de Endpoints REST

### 👤 Microservicio de Clientes (`http://localhost:8001`)

| Método | Endpoint | Descripción |
| :--- | :--- | :--- |
| `GET` | `/api/clients` | Listar todos los clientes registrados |
| `GET` | `/api/clients/{id}` | Consultar detalle de un cliente por su ID |
| `POST` | `/api/clients` | Registrar un nuevo cliente (`201 Created`) |
| `PUT` | `/api/clients/{id}` | Actualización completa de datos del cliente |
| `PATCH`| `/api/clients/{id}` | Actualización parcial del estado (`isActive`) |
| `DELETE`| `/api/clients/{id}` | Eliminar un cliente por ID |

### 💳 Microservicio de Cuentas y Transacciones (`http://localhost:8000`)

| Método | Endpoint | Descripción |
| :--- | :--- | :--- |
| `GET` | `/api/accounts` | Listar todas las cuentas bancarias |
| `GET` | `/api/accounts/{id}` | Obtener cuenta bancaria por su ID |
| `POST` | `/api/accounts` | Registrar una nueva cuenta (`201 Created`) |
| `PUT` | `/api/accounts/{id}` | Actualizar datos de una cuenta existente |
| `PATCH`| `/api/accounts/{id}` | Actualizar estado activo/inactivo de la cuenta |
| `DELETE`| `/api/accounts/{id}` | Eliminar una cuenta bancaria |
| `GET` | `/api/transactions` | Listar histórico general de transacciones |
| `GET` | `/api/transactions/{id}` | Consultar una transacción por su ID |
| `POST` | `/api/transactions` | Registrar transacción (depósito/retiro con cálculo de saldo) |
| `PUT` | `/api/transactions/{id}` | Actualizar datos de una transacción |
| `PATCH`| `/api/transactions/{id}` | Actualización parcial de una transacción |
| `DELETE`| `/api/transactions/{id}`| Eliminar registro de transacción |
| `GET` | `/api/transactions/clients/{clientId}/report` | **F4: Reporte de Estado de Cuenta** por fechas (`?dateTransactionStart=YYYY-MM-DD&dateTransactionEnd=YYYY-MM-DD`) |

---

## 🛡️ Arquitectura Bancaria Institucional: Concurrencia, Seguridad y Resiliencia

### 1. Concurrencia Distribuida y Bloqueo Pesimista (Pessimistic Locking)
* **Serialización a Nivel de Base de Datos**: Para soportar entornos cloud con múltiples réplicas (Kubernetes pods), se implementa bloqueo pesimista mediante `@Lock(LockModeType.PESSIMISTIC_WRITE)` (`SELECT ... FOR UPDATE`) sobre la entidad `Account` al procesar movimientos.
* **Eliminación de Condiciones de Carrera**: Ante peticiones concurrentes simultáneas de retiro sobre una misma cuenta, las transacciones se encolan a nivel de motor relacional, garantizando ACID absoluto y eliminando el riesgo de sobregiro o doble gasto (*double-spending*).
* **Rollback Seguro**: Al delegar la atomicidad a la transacción de base de datos (`@Transactional(isolation = Isolation.READ_COMMITTED)`), cualquier falla o cancelación revierte el estado sin dejar datos desincronizados en memoria.

### 2. Precisión Monetaria Estricta (`BigDecimal`)
* **Aritmética Financiera**: Todos los cálculos de saldo y movimientos contables se procesan utilizando `BigDecimal` con escala a 2 decimales y modo de redondeo bancario `RoundingMode.HALF_EVEN` (estándar bancario internacional).
* **Cero Desbordes Binarios**: Evita las inconsistencias y pérdidas de centavos características de los tipos flotantes binarios (`double`/`float`) bajo la norma IEEE-754.

### 3. Optimización de Consultas y Cumplimiento Normativo del Reporte (F4)
* **Consultas Agrupadas en Lote (Eliminación de N+1)**: En la generación de estados de cuenta consolidados (F4), se implementó la consulta indexada `findByAccountIdInAndDateBetween` combinada con agrupamiento en memoria (`groupingBy(Transaction::getAccountId)`). En lugar de ejecutar una consulta por cada cuenta del cliente (patrón N+1), se resuelven todos los movimientos de todas las cuentas asociadas en un único viaje de red (*single round-trip*) a la base de datos.
* **Cobertura Total de Cuentas (F4.1.1)**: Siguiendo estrictamente la cláusula *"Cuentas asociadas con sus respectivos saldos"*, el reporte incluye toda cuenta bancaria del cliente, reportando su saldo actual con movimiento en 0.0 incluso si no registró transacciones dentro del rango de fechas consultado.

### 4. Seguridad Bancaria y Buenas Prácticas (OWASP / PCI-DSS)
* **Mitigación de IDOR / Mass Assignment**: En las operaciones de creación (`POST`), el servicio garantiza la generación de claves primarias por el motor de base de datos forzando `id = null`, previniendo que un usuario malicioso sobrescriba registros preexistentes.
* **Validaciones de Integridad**: Manejo global de excepciones para violaciones de integridad (`DataIntegrityViolationException`) y argumentos ilegales (`IllegalArgumentException`), retornando respuestas estructuradas `400 Bad Request`.
* **Recomendaciones para Producción Bancaria**:
  - Implementación de `BCryptPasswordEncoder` / `Argon2id` para derivación de claves con salteado criptográfico.
  - Ocultación de credenciales en DTOs de salida (`@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)`).
  - Capa perimetral de seguridad con **Spring Security**, autenticación vía **OAuth2 / JWT** y autorización por roles (**RBAC**).

### 5. Comunicación Asincrónica y Patrón Transactional Outbox
* **Invocación Asincrónica No Bloqueante**: `ClientServiceClient` expone métodos asíncronos (`getClientNameAsync`) mediante `CompletableFuture`, permitiendo la resolución no bloqueante de datos entre microservicios.
* **Desacoplamiento Orientado a Eventos (EDA)**: Los eventos de dominio (`AccountDebitedEvent`, `AccountCreditedEvent`, `ClientRegisteredEvent`) se publican de forma asíncrona mediante un broker de mensajería (**Apache Kafka** o **RabbitMQ**).
* **Transactional Outbox**: Asegura consistencia eventual confiable escribiendo los eventos en una tabla `outbox` dentro de la misma transacción de base de datos antes de transmitirlos al bus de eventos, previniendo fallos de escritura dual (*dual-write problem*).

---

## 🧪 Pruebas Automatizadas

El proyecto incluye pruebas unitarias y de integración con **JUnit 5**, **Mockito** y **Spring Boot Test**:

* **Pruebas Unitarias de Dominio (F5)**: Validan la integridad de entidades de negocio, herencia entre `Person` y `Client`, y reglas de cálculo de balance.
* **Prueba de Excepción por Saldo Insuficiente (F3)**: Valida que un retiro mayor al saldo disponible genere la excepción `InsufficientFundsException` con el mensaje exacto `"Saldo no disponible"`.
* **Pruebas de Integración (F6)**: Ejecutan flujos end-to-end persistiendo en base de datos H2 en memoria y validando la interacción entre repositorios y servicios.

### Ejecución de Pruebas:
```bash
# Pruebas en el microservicio de clientes
cd client
./mvnw test

# Pruebas en el microservicio de cuentas
cd ../account
./mvnw test
```

---

## 🛠️ Instrucciones de Despliegue y Ejecución

### Opción 1: Ejecución Local con Maven Wrapper
Asegúrate de contar con Java 11 o Java 17 instalado en tu sistema.

**Terminal 1 (Microservicio Client):**
```bash
cd client
./mvnw spring-boot:run
```

**Terminal 2 (Microservicio Account):**
```bash
cd account
./mvnw spring-boot:run
```

---

### Opción 2: Ejecución Multi-contenedor con Docker Compose
Puedes levantar ambos microservicios de forma aislada y orquestada con un solo comando:

```bash
docker-compose up --build
```

---

## 📬 Colección de Postman

Se incluye el archivo listo para importar en Postman en la raíz del proyecto:
* **Ruta**: `collection_bank_postman.json`

Contiene todos los casos de prueba organizados por microservicio, incluyendo:
1. Creación de clientes (`Jose Lema`, `Marianela Montalvo`).
2. Creación de cuentas corrientes y de ahorro.
3. Transacciones de depósito y retiro con cálculo de balance.
4. Caso de prueba de error controlado (`Saldo no disponible` - 400).
5. Generación del reporte de estado de cuenta consolidado en formato JSON.
