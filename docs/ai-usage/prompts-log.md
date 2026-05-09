# Registro de uso de IA — notification-service

## Instrucciones de uso

Este archivo registra todas las interacciones con herramientas de IA
durante el desarrollo del proyecto. Debe actualizarse al finalizar
cada sesión de trabajo con Claude Code.

**Reglas:**
- Cada entrada tiene un identificador único secuencial (AI-001, AI-002, etc.)
- El prompt utilizado debe copiarse exactamente como fue enviado
- El resultado debe describir los archivos creados o modificados
- El commit hash debe completarse después de hacer el commit correspondiente
- Las capturas de pantalla se nombran con el identificador de la entrada

---

## Entradas completadas

### 🔷 AI-001 — Análisis inicial y diseño arquitectónico
- **Herramienta:** Claude (claude.ai)
- **Fecha:** 09/05/2026
- **Objetivo:** Analizar el challenge completo, debatir alternativas
  arquitectónicas y definir la solución antes de implementar
- **Prompt utilizado:** Ver captura `AI-001-prompt.png`
- **Resultado:**
    - Arquitectura hexagonal seleccionada sobre layered architecture
    - Stack tecnológico definido: Java 21, Spring Boot 3.5.14, Kafka,
      PostgreSQL, Resilience4j, WebClient
    - Estrategia de retry con exponential backoff y jitter definida
    - Plan de implementación completo por fases definido
    - Decisiones documentadas en Notion sección 1.3
- **Captura:** `AI-001-analisis-inicial.png`
- **Commit hash:** —

---

### 🔷 AI-002 — Generación del CLAUDE.md
- **Herramienta:** Claude (claude.ai)
- **Fecha:** 09/05/2026
- **Objetivo:** Generar el archivo maestro de contexto que Claude Code
  lee automáticamente al inicio de cada sesión
- **Prompt utilizado:**
```
  Genera el archivo CLAUDE.md que irá en la raíz del proyecto
  notification-service. Este archivo será leído automáticamente
  por Claude Code al inicio de cada sesión de trabajo.
  El contenido del archivo debe estar en inglés y en un mismo bloque
  markdown para poder copiar con facilidad.
  Debe incluir: rol y contexto del proyecto, stack tecnológico con
  versiones exactas, estructura de paquetes de la arquitectura hexagonal,
  reglas no negociables de código, ubicación de los archivos de specs
  en /docs/specs/, convenciones de commits semánticos a usar.
```
- **Resultado:** Archivo `CLAUDE.md` creado en la raíz del proyecto
  con contexto completo del sistema, stack, estructura de paquetes,
  reglas de código, convenciones de commits y ubicación de specs
- **Captura:** `AI-002-claude-md.png`
- **Commit hash:** ver `docs: add CLAUDE.md master context file for Claude Code sessions`

---

### 🔷 AI-003 — Generación de spec 01-system-design.md
- **Herramienta:** Claude (claude.ai)
- **Fecha:** 09/05/2026
- **Objetivo:** Especificar el diseño del sistema completo como input
  estructurado para Claude Code
- **Prompt utilizado:**
```
  Genera el archivo /docs/specs/01-system-design.md para el proyecto
  notification-service de Cobre, basándote en toda la arquitectura
  que definimos juntos en esta sesión. El contenido debe estar en inglés
  y en un solo bloque markdown.
  Debe incluir: descripción de los dos subsistemas, componentes principales,
  flujo completo de entrega, flujo de retry hasta el DLQ, criterios de
  aceptación en formato Given/When/Then, restricciones no funcionales.
```
- **Resultado:** Archivo `docs/specs/01-system-design.md` creado con
  descripción de ambos subsistemas, tabla de componentes, flujo de
  entrega paso a paso, flujo de retry, criterios Given/When/Then y
  restricciones de escalabilidad y resiliencia
- **Captura:** `AI-003-spec-01.png`
- **Commit hash:** ver `docs: add technical specs for all system components (01-07)`

---

### 🔷 AI-004 — Generación de spec 02-domain-model.md
- **Herramienta:** Claude (claude.ai)
- **Fecha:** 09/05/2026
- **Objetivo:** Especificar todas las entidades del dominio, invariantes
  de negocio y reglas de transición de estado
- **Prompt utilizado:**
```
  Genera el archivo /docs/specs/02-domain-model.md para el proyecto
  notification-service de Cobre, basándote en la arquitectura definida.
  El contenido debe estar en inglés y en un solo bloque markdown.
  Debe incluir: todas las entidades del dominio con sus campos y tipos
  exactos, el enum DeliveryStatus con todos sus estados y transiciones
  válidas, la entidad Subscription, el objeto NotificationEventFilter,
  el objeto DeliveryResult, invariantes de negocio, reglas de transición.
```
- **Resultado:** Archivo `docs/specs/02-domain-model.md` creado con
  `NotificationEvent`, `DeliveryStatus`, `Subscription`,
  `NotificationEventFilter`, `DeliveryResult`, 9 invariantes de
  negocio y máquina de estados completa
- **Captura:** `AI-004-spec-02.png`
- **Commit hash:** ver `docs: add technical specs for all system components (01-07)`

---

### 🔷 AI-005 — Generación de spec 03-hexagonal-structure.md
- **Herramienta:** Claude (claude.ai)
- **Fecha:** 09/05/2026
- **Objetivo:** Especificar las reglas estrictas de la arquitectura
  hexagonal, puertos, adaptadores y reglas de mapeo entre capas
- **Prompt utilizado:**
```
  Genera el archivo /docs/specs/03-hexagonal-structure.md para el proyecto
  notification-service de Cobre, basándote en la arquitectura definida.
  El contenido debe estar en inglés y en un solo bloque markdown.
  Debe incluir: explicación de cada capa y su responsabilidad, reglas
  estrictas de dependencia, descripción de cada puerto con su interfaz,
  descripción de cada adaptador, reglas de mapeo entre capas, ejemplos
  de qué está permitido y qué está prohibido en cada capa.
```
- **Resultado:** Archivo `docs/specs/03-hexagonal-structure.md` creado
  con interfaces completas de todos los puertos, descripción de los
  5 adaptadores, reglas de mapeo, tabla de permitido vs prohibido
  por capa
- **Captura:** `AI-005-spec-03.png`
- **Commit hash:** ver `docs: add technical specs for all system components (01-07)`

---

### 🔷 AI-006 — Generación de spec 04-api-contracts.md
- **Herramienta:** Claude (claude.ai)
- **Fecha:** 09/05/2026
- **Objetivo:** Definir los contratos completos de los tres endpoints
  con ejemplos reales basados en notification_events.json
- **Prompt utilizado:**
```
  Genera el archivo /docs/specs/04-api-contracts.md para el proyecto
  notification-service de Cobre, basándote en la arquitectura definida.
  El contenido debe estar en inglés y en un solo bloque markdown.
  Debe incluir: contrato completo de cada endpoint, request params con
  validaciones, response body para cada caso de respuesta, ejemplos
  reales usando los datos del notification_events.json, comportamiento
  de paginación, catálogo de errores.
```
- **Resultado:** Archivo `docs/specs/04-api-contracts.md` creado con
  contratos de los 3 endpoints, ejemplos curl reales con datos del
  JSON de ejemplo, catálogo completo de errores y comportamiento
  de paginación documentado
- **Captura:** `AI-006-spec-04.png`
- **Commit hash:** ver `docs: add technical specs for all system components (01-07)`

---

### 🔷 AI-007 — Generación de spec 05-webhook-delivery.md
- **Herramienta:** Claude (claude.ai)
- **Fecha:** 09/05/2026
- **Objetivo:** Especificar el mecanismo completo de entrega webhook,
  configuración de Resilience4j, Kafka y escenarios WireMock
- **Prompt utilizado:**
```
  Genera el archivo /docs/specs/05-webhook-delivery.md para el proyecto
  notification-service de Cobre, basándote en la arquitectura definida.
  El contenido debe estar en inglés y en un solo bloque markdown.
  Debe incluir: mecanismo de entrega via HTTPS, configuración exacta
  de Resilience4j, flujo detallado por intento, comportamiento del DLQ,
  configuración de Kafka, escenarios de test con WireMock,
  configuración del WebClient.
```
- **Resultado:** Archivo `docs/specs/05-webhook-delivery.md` creado
  con configuración completa de WebClient, parámetros exactos de
  Resilience4j, tabla de tiempos de backoff, configuración de Kafka
  con todos los topics y 6 escenarios WireMock documentados
- **Captura:** `AI-007-spec-05.png`
- **Commit hash:** ver `docs: add technical specs for all system components (01-07)`

---

### 🔷 AI-008 — Generación de spec 06-security.md
- **Herramienta:** Claude (claude.ai)
- **Fecha:** 09/05/2026
- **Objetivo:** Especificar las 3 vulnerabilidades OWASP, sus vectores
  de ataque concretos y la implementación de cada mitigación
- **Prompt utilizado:**
```
  Genera el archivo /docs/specs/06-security.md para el proyecto
  notification-service de Cobre, basándote en la arquitectura definida.
  El contenido debe estar en inglés y en un solo bloque markdown.
  Debe incluir: las 3 vulnerabilidades OWASP Top 10 con vector de ataque
  concreto en esta API, implementación exacta de mitigación en Spring Boot,
  configuración completa de Spring Security con OAuth2 JWT, rate limiting
  con Resilience4j, validación de ownership, casos de test de seguridad.
```
- **Resultado:** Archivo `docs/specs/06-security.md` creado con análisis
  de BOLA, Injection y Security Misconfiguration, código de mitigación
  completo para cada una, SecurityConfig, JwtClientIdExtractor,
  RateLimiter y suite de tests de seguridad
- **Captura:** `AI-008-spec-06.png`
- **Commit hash:** ver `docs: add technical specs for all system components (01-07)`

---

### 🔷 AI-009 — Generación de spec 07-observability.md
- **Herramienta:** Claude (claude.ai)
- **Fecha:** 09/05/2026
- **Objetivo:** Especificar métricas, logs estructurados, tracing
  distribuido y alertas para observabilidad en tiempo casi real
- **Prompt utilizado:**
```
  Genera el archivo /docs/specs/07-observability.md para el proyecto
  notification-service de Cobre, basándote en la arquitectura definida.
  El contenido debe estar en inglés y en un solo bloque markdown.
  Debe incluir: métricas Micrometer completas con tags, configuración
  de MDC para correlationId y clientId, configuración de Prometheus
  y Actuator, alertas clave con umbrales, OpenTelemetry para tracing,
  ejemplos de log estructurado, health indicators requeridos.
```
- **Resultado:** Archivo `docs/specs/07-observability.md` creado con
  tabla completa de métricas (contadores, timers, gauges), MDC filter,
  propagación en Kafka consumer, 3 ejemplos de logs JSON, configuración
  de Prometheus, 5 alertas con umbrales y KafkaHealthIndicator
- **Captura:** `AI-009-spec-07.png`
- **Commit hash:** ver `docs: add technical specs for all system components (01-07)`

---

### 🔷 AI-010 — Generación del prompts-log.md con todas las interacciones de diseño
- **Herramienta:** Claude (claude.ai)
- **Fecha:** 09/05/2026
- **Objetivo:** Generar prompts-log.md con todas las interacciones de la
  fase de diseño documentadas (AI-001 a AI-009), incluyendo template para
  futuras sesiones de Claude Code y tabla resumen
- **Prompt utilizado:** Ver captura `AI-010-prompt.png`
- **Resultado:** Archivo `docs/ai-usage/prompts-log.md` creado con 9 entradas
  documentadas (AI-001 a AI-009), template para sesiones de Claude Code y
  tabla resumen de interacciones
- **Captura:** `AI-010-prompts-log.png`
- **Commit hash:** `f74521f` — ver `docs: add AI usage log with all design phase interactions`

---

### 🔷 AI-011 — Implementación completa de la capa de dominio
- **Herramienta:** Claude Code
- **Fecha:** 09/05/2026
- **Objetivo:** Implementar la capa de dominio completa siguiendo la spec:
  modelos, excepciones, puertos de entrada y salida, y tests de transición
  de estado
- **Spec de referencia:** `02-domain-model.md`, `03-hexagonal-structure.md`
- **Prompt utilizado:**
```
  Read CLAUDE.md and /docs/specs/02-domain-model.md before doing anything.

  Your task is to implement the complete domain layer following the spec exactly.

  Before writing any code:
  1. List every file you will create with its full path
  2. Confirm that zero Spring or JPA annotations will appear
     in domain/model/ or domain/port/
  3. Wait for my approval

  Files to implement:
  - domain/model/NotificationEvent.java
  - domain/model/DeliveryStatus.java
  - domain/model/Subscription.java
  - domain/model/NotificationEventFilter.java
  - domain/model/DeliveryResult.java
  - domain/exception/EventNotFoundException.java
  - domain/exception/UnauthorizedAccessException.java
  - domain/exception/ReplayNotAllowedException.java
  - domain/exception/InvalidStatusTransitionException.java
  - domain/port/in/NotificationEventUseCase.java
  - domain/port/out/NotificationEventRepository.java
  - domain/port/out/WebhookDeliveryPort.java
  - domain/port/out/SubscriptionPort.java
  - domain/port/out/NotificationEventPublisher.java

  After implementation write unit tests for every valid and invalid
  status transition defined in the spec.
```
- **Archivos creados:**
  - `domain/model/DeliveryStatus.java`
  - `domain/model/NotificationEvent.java`
  - `domain/model/Subscription.java`
  - `domain/model/NotificationEventFilter.java`
  - `domain/model/DeliveryResult.java`
  - `domain/model/PageRequest.java` *(tipo puro Java añadido para paginación sin dependencia de Spring)*
  - `domain/model/PagedResult.java` *(tipo puro Java añadido para paginación sin dependencia de Spring)*
  - `domain/exception/EventNotFoundException.java`
  - `domain/exception/UnauthorizedAccessException.java`
  - `domain/exception/ReplayNotAllowedException.java`
  - `domain/exception/InvalidStatusTransitionException.java`
  - `domain/port/in/NotificationEventUseCase.java`
  - `domain/port/out/NotificationEventRepository.java`
  - `domain/port/out/WebhookDeliveryPort.java`
  - `domain/port/out/SubscriptionPort.java`
  - `domain/port/out/NotificationEventPublisher.java`
  - `domain/model/DeliveryStatusTransitionTest.java` *(test)*
- **Tests:** 40 passed, 0 failures, 0 errors — 5 clases anidadas cubriendo
  todas las transiciones válidas e inválidas a nivel de enum y de métodos
  de comportamiento del agregado. Sin Spring context cargado.
- **Captura:** `AI-011-domain-layer.png`
- **Commit hash:** `02c77a4` — ver `feat(domain): implement domain model, ports and status transition rules`

---

### 🔷 AI-012 — Implementación de NotificationEventService
- **Herramienta:** Claude Code
- **Fecha:** 09/05/2026
- **Objetivo:** Implementar `NotificationEventService` en `domain/service/`
  con los cuatro casos de uso del sistema: `findById`, `findAll`, `replay`
  y `processEvent`, incluyendo lógica de reintentos y publicación al DLQ
- **Spec de referencia:** `01-system-design.md`, `02-domain-model.md`
- **Prompt utilizado:**
```
  Read CLAUDE.md and /docs/specs/01-system-design.md and
  /docs/specs/02-domain-model.md before doing anything.

  Your task is to implement NotificationEventService in
  domain/service/ following the specs exactly.

  Before writing any code:
  1. List every file you will create with its full path
  2. Confirm the service only depends on domain ports and
     domain model, zero infrastructure imports
  3. Wait for my approval

  The service must implement NotificationEventUseCase and handle:
  - findAll: apply filter, enforce clientId from parameter
    (never trust external input), return PagedResult
  - findById: load event, validate clientId ownership,
    throw UnauthorizedAccessException if mismatch
  - replay: validate status is FAILED, reset to PENDING via
    resetForReplay(), publish to pending topic
  - processEvent: check subscription via SubscriptionPort,
    if no match mark as SKIPPED, if match attempt delivery
    via WebhookDeliveryPort wrapped in retry logic,
    update status based on DeliveryResult, persist result

  After implementation write unit tests for every method covering:
  - Happy path
  - Event not found
  - Unauthorized access (wrong clientId)
  - Replay on non-FAILED event
  - Subscription not found (SKIPPED)
  - All retries exhausted (FAILED + DLQ publish)
```
- **Archivos modificados:**
  - `domain/port/in/NotificationEventUseCase.java` *(añadido `processEvent(NotificationEvent event)`)*
- **Archivos creados:**
  - `domain/service/NotificationEventService.java`
  - `domain/service/NotificationEventServiceTest.java` *(test)*
- **Tests:** 58 passed, 0 failures, 0 errors — 18 tests nuevos en 4 clases
  anidadas (`findById`, `findAll`, `replay`, `processEvent`) con Mockito strict
  stubbing. Sin Spring context cargado. Acumulado con los 40 de AI-011.
- **Captura:** `AI-012-notification-event-service.png`
- **Commit hash:** `[pendiente — completar con hash de feat(domain): implement NotificationEventService]`

---

### 🔷 AI-013 — Implementación del adaptador de persistencia
- **Herramienta:** Claude Code
- **Fecha:** 09/05/2026
- **Objetivo:** Implementar el adaptador de persistencia completo en
  `adapter/out/persistence/`, incluyendo entidad JPA, repositorio Spring Data,
  Specifications para filtros, mapper dominio↔JPA, el adaptador que implementa
  el puerto `NotificationEventRepository`, migración Flyway, datos semilla y
  `DataInitializer` para carga al inicio
- **Spec de referencia:** `02-domain-model.md`, `03-hexagonal-structure.md`
- **Prompt utilizado:**
```
  Read CLAUDE.md and /docs/specs/02-domain-model.md and
  /docs/specs/03-hexagonal-structure.md before doing anything.

  Your task is to implement the persistence adapter following
  the specs exactly.

  Before writing any code:
  1. List every file you will create with its full path
  2. Confirm that JPA annotations stay inside adapter/out/persistence/
     and never leak into domain/model/
  3. Wait for my approval

  Files to implement:
  - adapter/out/persistence/NotificationEventJpaEntity.java
  - adapter/out/persistence/NotificationEventJpaRepository.java
  - adapter/out/persistence/NotificationEventSpecification.java
  - adapter/out/persistence/NotificationEventJpaMapper.java
  - adapter/out/persistence/NotificationEventPersistenceAdapter.java
  - src/main/resources/db/migration/V1__create_notification_events.sql
  - src/main/resources/data/notification_events.json
  - config/DataInitializer.java

  After implementation write an integration test using
  Testcontainers PostgreSQL covering:
  - Save and retrieve an event by ID
  - Filter by clientId, status, date range
  - Pagination returns correct page size and total elements
  - DataInitializer loads seed data correctly on empty DB
```
- **Archivos creados:**
  - `adapter/out/persistence/NotificationEventJpaEntity.java`
  - `adapter/out/persistence/NotificationEventJpaRepository.java`
  - `adapter/out/persistence/NotificationEventSpecification.java`
  - `adapter/out/persistence/NotificationEventJpaMapper.java`
  - `adapter/out/persistence/NotificationEventPersistenceAdapter.java`
  - `config/DataInitializer.java`
  - `src/main/resources/db/migration/V1__create_notification_events.sql`
  - `src/main/resources/data/notification_events.json` *(8 eventos semilla: CLIENT001 × 5, CLIENT002 × 3, todos los estados representados)*
  - `adapter/out/persistence/NotificationEventPersistenceAdapterIT.java` *(test)*
- **Archivos modificados:**
  - `pom.xml` *(añadidos `spring-boot-testcontainers`, `testcontainers:postgresql`, `testcontainers:junit-jupiter`)*
- **Tests:** 68 passed, 0 failures, 0 errors — 10 tests de integración nuevos
  en 5 clases anidadas (`saveAndFindById`, `FilterByClientId`, `FilterByStatus`,
  `FilterByDateRange`, `PaginationAndSort`, `DataInitializer`) con `@DataJpaTest`
  + Testcontainers PostgreSQL 15. Acumulado con los 58 de sesiones anteriores.
- **Captura:** `AI-013-persistence-adapter.png`
- **Commit hash:** `[pendiente — completar con hash de feat(persistence): implement JPA persistence adapter with Testcontainers integration test]`

---

### 🔷 AI-014 — Implementación del adaptador REST de entrada
- **Herramienta:** Claude Code
- **Fecha:** 09/05/2026
- **Objetivo:** Implementar el adaptador REST completo en `adapter/in/rest/`:
  controlador con los tres endpoints, DTOs de respuesta, mapper dominio→DTO,
  manejador global de excepciones, `SecurityConfig` con OAuth2 JWT resource
  server, rate limiting por `clientId` con Resilience4j, y suite de tests
  MockMvc con `spring-security-test`
- **Spec de referencia:** `04-api-contracts.md`, `06-security.md`
- **Prompt utilizado:**
```
  Read CLAUDE.md and /docs/specs/04-api-contracts.md and
  /docs/specs/06-security.md before doing anything.

  Your task is to implement the REST input adapter following
  the specs exactly.

  Before writing any code:
  1. List every file you will create with its full path
  2. Confirm that clientId is always extracted from JWT,
     never from request parameters
  3. Confirm that replay returns 202 Accepted, never 200
  4. Wait for my approval
```
- **Archivos creados:**
  - `adapter/in/rest/dto/NotificationEventResponse.java`
  - `adapter/in/rest/dto/PagedNotificationEventResponse.java` *(con `PageMetadata` anidado)*
  - `adapter/in/rest/dto/ReplayResponse.java`
  - `adapter/in/rest/mapper/NotificationEventRestMapper.java`
  - `adapter/in/rest/InvalidTokenException.java` *(excepción adaptador para JWT sin claim `clientId` → 401)*
  - `adapter/in/rest/NotificationEventController.java`
  - `adapter/in/rest/GlobalExceptionHandler.java` *(con `ErrorResponse` anidado)*
  - `config/SecurityConfig.java`
  - `adapter/in/rest/NotificationEventControllerIT.java` *(test)*
- **Archivos modificados:**
  - `pom.xml` *(añadidos `spring-boot-starter-oauth2-resource-server` y `resilience4j-spring-boot3:2.2.0`)*
  - `src/main/resources/application.yaml` *(añadidos `jwk-set-uri`, configuración default de rate limiter y endpoints de Actuator)*
- **Decisiones técnicas:**
  - `clientId` extraído exclusivamente de `jwt.getClaimAsString("clientId")` en método privado `extractClientId`
  - Replay retorna `ResponseEntity.accepted()` (202), nunca 200
  - Rate limiting programático vía `rateLimiterRegistry.rateLimiter("replay-endpoint-" + clientId)` por cliente
  - `GlobalExceptionHandler` mapea: `EventNotFoundException` → 404, `UnauthorizedAccessException` → 403,
    `ReplayNotAllowedException` → 400, `RequestNotPermitted` → 429, `IllegalArgumentException` → 400 (incluye rango de fechas inválido),
    `MethodArgumentTypeMismatchException` → 400 (enum status inválido), `InvalidTokenException` → 401
  - `@MockBean JwtDecoder` en tests para satisfacer la dependencia de `SecurityConfig` sin issuer real
  - `RateLimiter` mockeado vía `doAnswer` para invocar el supplier en el camino feliz y `doThrow` para 429
- **Tests:** 82 passed, 0 failures, 0 errors — 14 tests nuevos en 3 clases anidadas
  (`FindAll` × 5, `FindById` × 4, `Replay` × 5) con `@WebMvcTest` + `@Import(SecurityConfig.class)`
  + JWT post-processors de `spring-security-test`. Acumulado con los 68 de sesiones anteriores.
- **Captura:** `AI-014-rest-adapter.png`
- **Commit hash:** `[pendiente — completar con hash de feat(api): implement REST input adapter with security and rate limiting]`

---

## Template para próximas entradas (Claude Code)

Copiar y completar para cada sesión de Claude Code:

```
### 🔷 AI-0XX — [Título de la sesión]
- **Herramienta:** Claude Code
- **Fecha:** DD/MM/YYYY
- **Objetivo:** [Qué se implementó en esta sesión]
- **Spec de referencia:** [Nombre del archivo .md leído]
- **Prompt utilizado:**
  [Copiar el prompt exacto enviado a Claude Code]
- **Archivos creados:**
  - [lista de archivos nuevos]
- **Archivos modificados:**
  - [lista de archivos modificados]
- **Tests:** [Resultado de los tests: X passed, Y failed]
- **Captura:** [nombre del archivo de captura]
- **Commit hash:** [hash del commit]
```

---

## Tabla resumen de interacciones

| ID | Herramienta | Objetivo | Archivos generados | Fecha |
|---|---|---|---|---|
| AI-001 | Claude | Análisis inicial y arquitectura | — (decisiones en Notion) | 09/05/2026 |
| AI-002 | Claude | CLAUDE.md | `CLAUDE.md` | 09/05/2026 |
| AI-003 | Claude | Spec system design | `docs/specs/01-system-design.md` | 09/05/2026 |
| AI-004 | Claude | Spec domain model | `docs/specs/02-domain-model.md` | 09/05/2026 |
| AI-005 | Claude | Spec hexagonal structure | `docs/specs/03-hexagonal-structure.md` | 09/05/2026 |
| AI-006 | Claude | Spec API contracts | `docs/specs/04-api-contracts.md` | 09/05/2026 |
| AI-007 | Claude | Spec webhook delivery | `docs/specs/05-webhook-delivery.md` | 09/05/2026 |
| AI-008 | Claude | Spec security | `docs/specs/06-security.md` | 09/05/2026 |
| AI-009 | Claude | Spec observability | `docs/specs/07-observability.md` | 09/05/2026 |
| AI-010 | Claude | prompts-log.md con interacciones de diseño | `docs/ai-usage/prompts-log.md` | 09/05/2026 |
| AI-011 | Claude Code | Implementación capa de dominio | 16 archivos en `domain/model`, `domain/exception`, `domain/port` | 09/05/2026 |
| AI-012 | Claude Code | Implementación NotificationEventService | `domain/service/NotificationEventService.java`, `NotificationEventServiceTest.java` | 09/05/2026 |
| AI-013 | Claude Code | Implementación adaptador de persistencia | 9 archivos en `adapter/out/persistence`, `config`, `db/migration`, `data` | 09/05/2026 |
| AI-014 | Claude Code | Implementación adaptador REST de entrada | 9 archivos en `adapter/in/rest`, `config`; modificados `pom.xml` y `application.yaml` | 09/05/2026 |