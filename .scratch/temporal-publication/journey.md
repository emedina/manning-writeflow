# Publication journey

## Commit 1: the naive dual write

### Failure to feel

The publish handler now calls the Distribution Platform from inside its database transaction. The platform can accept the Article, then the database save can fail. The database transaction cannot undo the platform call, so WriteFlow still has a Draft while the platform has the Article.

### Concept

`@Transactional` covers the local database work. It does not make a remote effect part of that transaction. This change deliberately shows the mismatch before the later commits move sequencing and recovery outside the handler.

### Change

`PublishArticleHandler` calls `DistributionPlatformOutputPort.distribute` after the Article transition and before `ArticleRepository.save`. The port receives `ArticleForDistributionDTO` with the Article id, title, content and author id. The new fake adapter remembers distributed ids, treats repeated distribution as success, and can return a programmed `DistributionRejected`. `withdraw` is idempotent by Article id and accepts the id alone. The API maps `DistributionRejected` to 409. The request still supplies the publication date.

The handler's ordered `Either` chain is: validate the article id, validate the author id, read the Article, call `article.publish(authorId, command.publicationDate())`, map the published Article to `ArticleForDistributionDTO`, call `distribute`, save the Article, then publish its domain events. The handler retains its kernel `@Transactional` annotation. A left short-circuits later steps. Thus a left from `save` prevents event publication, but it cannot undo a successful platform call.

### What the reader sees

The handler test `handleShouldLeaveOrphanAtPlatformWhenSaveFailsAfterDistribution` records the Article accepted by the platform, keeps a Draft snapshot for the failed database save, checks the call order and confirms the handler returns the persistence error. The fake adapter tests cover repeated distribution, a programmed failure, and withdrawing an Article that was never distributed. This is the intentionally incomplete first state; it has no repair path. It is not the target architecture: ticket 02 removes the remote call from this handler as the process is split into local steps, ticket 03 adds durable execution, and ticket 04 hands the start event to Temporal through the outbox. The orphan test leaves with ticket 02, while this commit and its failure remain in history as the before-state evidence.

### Decisions and evidence

- Kept the external call in the handler transaction because this step is meant to show the Part 1 reader's natural extension, not the final design.
- Preserved `publicationDate` in the request and existing domain transition. Ticket 01 does not reshape the publication process.
- Used an output port owned by the core and a DTO in the shared kernel. The fake adapter depends on the port and shared kernel, not on the handler or domain.
- Kept the new business refusal in the sealed `Error` hierarchy and completed the exhaustive API mapping with a 409 conflict.
- Kept both platform operations on one role port: `distribute(ArticleForDistributionDTO)` and `withdraw(String articleId)`, each returning `Either<Error, Void>`. The DTO fields are `id`, `title`, `content`, and `authorId`. `DistributionRejected` carries `articleId` and `reason`. The fake's `failNextWith(error)` affects one distribution call; withdrawal has no business failures.
- Put the fake in the `distribution-platform-adapter` module. It is a package-private kernel `@Adapter`, logs instead of calling a service, and stores distributed ids in an in-memory set. The assembly includes its adapter package in the ArchUnit configuration.
- Ticket and spec evidence: `.scratch/temporal-publication/issues/01-naive-dual-write.md` and `.scratch/temporal-publication/spec.md`, especially the first narrative step, output-port contract, and fake-adapter decision.
- Code trail: handler `application-core/application/src/main/java/com/manning/writeflow/application/PublishArticleHandler.java`; port `application-core/output-ports/src/main/java/com/manning/writeflow/application/ports/out/DistributionPlatformOutputPort.java`; DTO `shared-kernel/src/main/java/com/manning/writeflow/shared/dto/ArticleForDistributionDTO.java`; fake `distribution-platform-adapter/src/main/java/com/manning/writeflow/external/distribution/FakeDistributionPlatformAdapter.java`; error hierarchy `shared-kernel/src/main/java/com/manning/writeflow/shared/error/Error.java`; HTTP mapping `rest-adapter/src/main/java/com/manning/writeflow/api/ApiErrorHandler.java`.
- Test trail: orphan scenario `application-core/application/src/test/java/com/manning/writeflow/application/PublishArticleHandlerTest.java`, test `handleShouldLeaveOrphanAtPlatformWhenSaveFailsAfterDistribution`; fake behaviors `distribution-platform-adapter/src/test/java/com/manning/writeflow/external/distribution/FakeDistributionPlatformAdapterTest.java`; refusal mapping `rest-adapter/src/test/java/com/manning/writeflow/api/ApiErrorHandlerTest.java`; DTO fields `shared-kernel/src/test/java/com/manning/writeflow/shared/dto/ArticleForDistributionDTOTest.java`.
- Test evidence: `mvn clean verify` passed on JDK 25. All 13 reactor modules succeeded; the build reported 17 tests, with no failures, errors or skips. The handler test verifies call order and the failed save result.

### Check yourself

If `save` returns a left after `distribute` returns a right, which system can still say the Article is distributed?

## Commit 2: publication is accepted, not completed (working draft)

### Failure to feel

The handler called the Distribution Platform before the database commit. Ticket 01 showed that the platform could accept
the Article while the database save failed. This step removes that call from the trigger and records the accepted request
as `PUBLISHING` instead.

### Concept

The request records a local state change and acknowledges that work has started. `202 Accepted` does not mean that the
Distribution Platform has the Article. The state stays `PUBLISHING` until a later step confirms or reverts it.

### Change

`ArticleStatus.PUBLISHING` now marks an accepted request. `startPublication` enforces ownership and emits
`PublicationStarted`; `confirmPublication` and `revertPublication` define the later domain transitions, including
idempotent completion/reversion and explicit refusal errors. The handler now only starts the transition, saves, then
publishes domain events. The publication date is removed from the request and command. REST responds `202 Accepted`
with the article URI in `Location`. The remote platform call and ticket-01 orphan scenario are removed; the port and fake
adapter remain for the later distribution step.
Persistence adds `@Version` and updates the entity loaded during save; optimistic-lock errors are translated at the
persistence boundary when raised there and are mapped to HTTP 409 if they escape transaction completion.

### Decisions and evidence

- Domain transitions emit facts at their corresponding transition: starting raises `PublicationStarted`; confirming
  raises `ArticlePublished`; reverting raises no event. Idempotent operations raise no duplicate fact.
- The handler retains its existing name and kernel transaction boundary, but no longer contacts a distribution
  platform. Ticket 01's failure remains documented above as historical before-state evidence.
- Locking behavior is described as a manual running-application demonstration; no automated concurrent transaction
  test was added.
- `mvn clean verify` passed on JDK 25 after the implementation and test updates. The manual run required by the ticket
  (POST returns 202, subsequent GET remains PUBLISHING) was **not performed** in this implementation session and remains
  pending.
- Changed code trail includes Article transitions and event, command/handler, REST request and response, error
  mappings, and versioned persistence. Transition and handler tests cover the state matrix, ownership guard, event
  publication order and rejected start. The final full reactor run passed 153 tests, with no
  failures, errors or skips. The manual run is the only pending evidence.

### What the reader sees

The controller returns `202 Accepted` and a `Location` for the Article. A GET returns `PUBLISHING`; no handler or
background process advances it in this commit.

### Check yourself

Why does a `202 Accepted` response leave the domain state at `PUBLISHING` instead of `PUBLISHED`?

## Commit 2, step 2: step and compensation use cases (working draft)

### Change and decisions

The Article can now advance one Publication step per command. `DistributeArticleHandler` reads the Article, maps its
fields to `ArticleForDistributionDTO`, and calls the Distribution Platform without opening a transaction.
`ConfirmPublicationHandler` confirms and saves inside a transaction, then publishes the Article's pending domain
events. `RevertPublicationHandler` reverts and saves transactionally. `WithdrawArticleHandler` delegates to the
Distribution Platform without a transaction. The handlers sequence ports and aggregate methods; the Article retains
the transition rules and idempotency.

The confirming command takes `publishedAt` as an `Instant`; the aggregate does not read a clock. The confirm handler
publishes the event list drained after save, so a fresh confirmation publishes its `ArticlePublished` event and a
repeat on an already Published Article supplies an empty list. A failed read or domain transition short-circuits later
ports. No transaction surrounds either Distribution Platform operation.

### Evidence trail

- Added `DistributeArticleCommand`, `ConfirmPublicationCommand`, `RevertPublicationCommand`, and
  `WithdrawArticleCommand`, each with `validateThenCreate`, plus matching use-case interfaces and handlers.
- Added command validation tests and handler tests using mocked ports with the real Article aggregate. They cover
  distribution mapping, confirmation event contents and idempotent confirmation, reversion, withdrawal delegation,
  and `never()` assertions after short-circuit failures.
- Source review confirms the four handlers contain no business `if`. Transactions appear only on confirm and revert.
- The first full build exposed an existing mismatch in EPO `InputPortChecker`: `CommandHandler<E, C>` puts the Command
  second, while the checker tests the first type argument. Existing command use cases had the same frozen violation.
  I kept the valid handler signature and added the four new instances to that same baseline.
- Two follow-up builds caught missing test-fixture imports/constants after test cleanup. I restored those fixtures before
  the passing run.
- `mvn clean verify` then passed on Corretto 25. All 13 reactor modules succeeded; 214 tests passed with no failures,
  errors or skips. JaCoCo passed for input-ports and application; the assembly module skips coverage by configuration.
  All nine ArchUnit tests passed. The Maven run was delegated as required by the repository guidance.

## Commit 2, step 3: publication request reaches the outbox (working draft)

### Failure to feel

The trigger now commits the Article as `PUBLISHING`, but without an outbox row a process failure can leave no durable
record for the later workflow handoff. The existing `ArticlePublished` listener also logs a failed outbox write and lets
its transaction continue, so the `Published` state can commit without its fact.

### Concept

The state change and the fact that caused it must commit together. A `BEFORE_COMMIT` listener dispatches a recording use
case in the trigger transaction. If that use case returns a left, the listener throws and prevents the commit. The
`PublicationStarted` fact means an Author requested publication; it does not mean the Article is Published.

### Change

`RecordPublicationStartedCommand` validates the Article and Author identifiers, and its use case and transactional
handler call the new `PublicationStarted` overload on `IntegrationEventPublisherOutputPort`. The outbox adapter creates
an event id and creation time, serializes the Article id with them, and saves the row with event type
`PublicationStarted` and `published_at` unset. The new listener dispatches that command before commit and throws on a
left. `RecordIntegrationEventListener` now throws on a left as well. No code reads the new row or starts a workflow in
this step.

### What the reader sees

The source path makes the request durable alongside the `PUBLISHING` transition; an outbox recording failure aborts the
transaction rather than leaving state without its fact. The automated adapter test checks the event id, Article id,
creation time, event type and null `published_at`. The listener tests check command dispatch and throwing on failure.

### Decisions and evidence

- Kept a sibling command, use case, handler and listener for `PublicationStarted`, matching the Part 1 recording path.
  The port receives the existing domain event rather than a persistence row or an engine-specific message.
- Stored only event id, Article id and creation time in the integration-event payload. Its Javadoc states that the
  request is not proof of publication, as required by ADR-0002 and ADR-0004.
- Made both outbox listeners throw on a left. The `RecordIntegrationEventListener` logging-only behavior could otherwise
  allow a Published Article without its outbox fact.
- Used `Try.run(...).toEither()` instead of `try/catch` in the outbox publisher methods. Only Jackson and database
  failures become `PersistenceError`; unexpected failures remain exceptions with their cause.
- Focused test evidence added: `RecordPublicationStartedCommandTest` covers valid identifiers, invalid identifiers and
  accumulated failures; `RecordPublicationStartedHandlerTest` checks reconstruction and port dispatch;
  `OutboxIntegrationEventPublisherTest.shouldWritePublicationStartedOutboxRow` checks the new payload and row fields;
  `RecordPublicationStartedListenerTest` checks dispatch and failure propagation; and
  `RecordIntegrationEventListenerTest.onShouldThrowWhenRecordingArticlePublicationFails` checks the Part 1 listener's
  failure path.
- Manual application run evidence: creating an Article returned 201; the first publication request returned 202 with a
  `Location` header; a second request returned 409. Querying the H2 outbox found exactly one `PublicationStarted` row
  for the Article, with `published_at` null. The rejected request added no row. The run used an isolated temporary copy,
  and its in-memory database and temporary files were removed after inspection.
- `mvn clean verify` passed with Amazon Corretto 25.0.0.36.2 and Maven 3.9.12. All 13 reactor projects succeeded; all
  tests passed with no failures, errors or skips.
- The first full build found the EPO `InputPortChecker` generic-position false positive for the new
  `RecordPublicationStartedUseCase`. Like ticket 03, the valid `CommandHandler<Error, Command>` signature was kept; the
  frozen checker store now includes only this use case under the existing false-positive rule. The focused checker
  passed after the update, and the full build passed without refreezing.

### Check yourself

What can a consumer conclude from a `PublicationStarted` row, and what must it wait to learn from `ArticlePublished`?
