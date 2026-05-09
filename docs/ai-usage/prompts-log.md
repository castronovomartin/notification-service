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