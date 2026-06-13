# WriteFlow

> The reference implementation for **Chapter 9** of the Manning book on **Hexagonal Architecture** — a complete, runnable, fully-tested article-publishing service built strictly inside-out, from the domain to the composition root.

WriteFlow is not a toy. It is a small but *honest* hexagonal application: every architectural rule the book teaches is not only followed but **enforced on every build**. The dependencies point inward because a test fails if they do not. The domain stays always-valid for the same reason. This repository is the worked example that chapter 9 constructs piece by piece.

---

## What it does

WriteFlow models the lifecycle of an article through three use cases:

| Endpoint | Use case | Result |
| --- | --- | --- |
| `POST /api/articles` | Create a draft article | `201 Created` + server-generated id, status `DRAFT` |
| `POST /api/articles/{id}/publication` | Publish an existing draft | `204 No Content` |
| `GET /api/articles/{id}` | Retrieve an article | `200 OK` + article representation |

Publishing an article raises a domain event that is routed through two transaction-phase listeners — one that records an integration event into a **transactional outbox** (before commit, atomically with the business change), and one that fires a notification (after commit, once the change is durable).

---

## Why it exists

The book teaches hexagonal architecture inside-out, and this codebase is the destination. It demonstrates how a handful of disciplined decisions compose into an application that is **both well-structured and demonstrably correct**:

- **An always-valid aggregate, with a guarded reconstitution path.** `Article` is *created* only through `validateThenCreate` (which enforces every invariant) and *rehydrated* only through `reconstitute` (which trusts persisted data and raises no events). A **custom ArchUnit rule** — not a visibility modifier — guarantees `reconstitute` is reachable only from the persistence adapter.
- **Either-only error handling, all the way out.** Value objects, the entity, commands, queries, and output ports all return `Either<Error, T>`. Expected failures travel as left values along functional rails from the core to the controller, where an exhaustive `switch` over a sealed `Error` hierarchy maps each one to an HTTP status — with no `default` branch, so a new error variant cannot compile until someone decides its status. Exceptions are reserved for the genuinely exceptional.
- **Ports that hide technology behind contracts.** Every output port carries the `OutputPort` suffix; every adapter is `@Adapter` and package-private. Substituting one adapter for another — a fake notification for a real email gateway, H2 for a production database — is an assembly-time decision that touches no core code.
- **The transactional outbox.** Publishing an article and recording its integration event happen in **one transaction**. The published-article fact lives in three representations — domain event, integration event, outbox row — each serving a different audience and changing for a different reason.
- **Stereotype-driven assembly.** The composition root wires beans by architectural *role* (`@ApplicationService`, `@Adapter`, `@DomainService`), keeping framework annotations out of the inner layers. The command and query buses derive their routing tables from the handlers' generic types — adding a use case requires no edit to the composition root.
- **Architecture *and* behaviour, verified on every build.** A reusable ArchUnit checker library proves the structure is hexagonal; a suite of unit tests proves the behaviour is correct. `mvn clean verify` is green only when both hold.

---

## Architecture at a glance

WriteFlow is a multi-module Maven project. Each module maps to a ring of the hexagon, and the Maven boundaries make the dependency rules physical: an inner module simply cannot import an outer one.

```
                        ┌─────────────────────────────────────────────┐
                        │            spring-boot-assembly               │
                        │   (composition root — wires everything,       │
                        │    owns the ArchUnit verification suite)      │
                        └─────────────────────────────────────────────┘
                                          │ depends on
        ┌──────────────┬──────────────┬───┴──────────┬──────────────────┐
        ▼              ▼              ▼               ▼                  ▼
  ┌───────────┐  ┌────────────┐ ┌──────────────┐ ┌──────────────┐ ┌────────────────────┐
  │   rest-   │  │persistence-│ │event-listener│ │ notification │ │   (driven + driving │
  │  adapter  │  │  adapter   │ │  -adapter    │ │  -adapter    │ │     adapters)       │
  │ (driving) │  │  (driven)  │ │  (driven)    │ │  (driven)    │ │                     │
  └───────────┘  └────────────┘ └──────────────┘ └──────────────┘ └────────────────────┘
        │              │              │               │
        └──────────────┴──────┬───────┴───────────────┘ depend inward on
                              ▼
                     ┌──────────────────┐
                     │ application-core  │
                     │  • domain         │  ← the always-valid aggregate, VOs, events
                     │  • input-ports    │  ← use-case interfaces, commands, queries
                     │  • output-ports   │  ← contracts the adapters implement
                     │  • application    │  ← handlers (orchestration, no business logic)
                     └──────────────────┘
                              │ depends on
                              ▼
                     ┌──────────────────┐
                     │   shared-kernel   │  ← Error hierarchy, validation, DTOs, events
                     └──────────────────┘
```

| Module | Ring | Responsibility |
| --- | --- | --- |
| `shared-kernel` | Foundation | `Error` hierarchy, validation primitives, DTOs, domain-event marker |
| `application-core` | Inner | Domain (`domain`), use-case ports (`input-ports`), driven ports (`output-ports`), handlers (`application`) |
| `rest-adapter` | Driving | The single inbound adapter — HTTP controller, request/response records, error-to-ProblemDetail mapping |
| `persistence-adapter` | Driven | Spring Data JPA repository, outbox writer, in-process event bridge |
| `event-listener-adapter` | Driven | Transaction-phase listeners (`BEFORE_COMMIT` outbox, `AFTER_COMMIT` notify) |
| `notification-adapter` | Driven | Fake notification stand-in (swappable for a real email gateway) |
| `spring-boot-assembly` | Composition root | Bootstraps Spring, wires beans by stereotype, stands up the buses, and **owns the architecture-test suite** |

---

## Tech stack

| | |
| --- | --- |
| **Language** | Java 25 |
| **Framework** | Spring Boot 4.0.6 |
| **Functional core** | Vavr 0.11.0 (`Either`-based error handling) |
| **Mapping** | MapStruct 1.6.3 |
| **Boilerplate** | Lombok 1.18.46 |
| **Serialization** | Jackson 3 (`tools.jackson.*`, Spring Boot 4 native) |
| **API docs** | springdoc-openapi 3.0.3 |
| **Database** | H2 (in-memory, for the runnable example) |
| **Testing** | JUnit 6, Mockito 5, AssertJ, ArchUnit |
| **Architecture Kernel** | `io.github.emedina` shared-kernel, command/query buses, ArchUnit hexagonal checkers (v1.0.0) |

---

## Running it

**Requirements:** JDK 25 and Maven.

```bash
# Build, run all unit tests, and execute the full ArchUnit verification suite
mvn clean verify

# Start the application (H2 in-memory, port 8086)
mvn -pl spring-boot-assembly spring-boot:run
```

The full lifecycle, end to end:

```bash
# 1. Create a draft — returns 201 with a server-generated id
curl -i -X POST http://localhost:8086/api/articles \
  -H 'Content-Type: application/json' \
  -d '{"authorId":"22222222-2222-2222-2222-222222222222","title":"Hexagons all the way down","content":"..."}'

# 2. Publish it — returns 204 (use the id from step 1)
curl -i -X POST http://localhost:8086/api/articles/{id}/publication \
  -H 'Content-Type: application/json' \
  -d '{"authorId":"22222222-2222-2222-2222-222222222222","publicationDate":"2026-06-13T10:15:30Z"}'

# 3. Retrieve it — returns 200 with status PUBLISHED
curl -i http://localhost:8086/api/articles/{id}
```

OpenAPI / Swagger UI is available via springdoc once the application is running.

---

## Verifying the architecture

The architecture is an **executable specification**, not a diagram. The verification suite lives in `spring-boot-assembly/src/test`:

- **Eight role checkers** reuse the shared ArchUnit hexagonal library via `ArchTests.in(...)` — one each for the domain, commands, queries, handlers, input ports, output ports, shared kernel, and adapters.
- **One project-specific rule** (`ReconstitutionAccessArchitectureTest`) encodes a decision unique to WriteFlow: `Article.reconstitute` may be called only from the persistence adapter. It breaks the build the moment any other class calls it.

```bash
mvn -pl spring-boot-assembly test
```

A broken dependency rule fails the build just as surely as a broken business rule.

---

## About the book

This repository accompanies the Manning book on hexagonal architecture. **Part One** builds a single hexagon from the inside out (domain → ports → handlers → adapters → assembly → enforcement → testing → this full worked example). **Part Two — _The Hexagon in a Distributed World_** — takes these same patterns and scales them across services: integration modes, anti-corruption layers, event-driven integration (where this outbox finally reaches a consumer), workflow-engine-driven hexagons, and multi-team ownership.

The patterns you see here do not change in Part Two. They become the building blocks of something larger.

---

## Author

Enrique Medina Montenegro
