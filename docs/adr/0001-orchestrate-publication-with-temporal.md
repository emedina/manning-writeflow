Status: accepted

# Orchestrate the Publication with Temporal

Publishing an article now has to reach the Distribution Platform as well as our own database. A handler that calls
the platform inside its database transaction only works when nothing fails: the transaction can roll back the
database and cannot roll back the remote call, and a crash between the two leaves no record of what was in flight.
We moved the sequencing, retrying and compensating of the Publication steps to Temporal, a durable-execution engine,
and kept every rule, state and fact inside the hexagon.

## Why a handler cannot be the orchestrator here

The Part 1 handler orchestrates local, synchronous steps inside one transaction, and that is still correct for
local steps. Once a step crosses to another system the handler cannot do three things the process needs: survive its
own crash and resume, wait and retry with backoff over minutes or hours, and undo a remote effect that already
completed. These are properties of the problem, not of any tool. Any correct solution (an outbox with a consumer, a
state table with a poller, an engine) decomposes the use case into the same steps: move local state, call the
platform, confirm local state, with an undo for each.

## Considered options

| Option                                                                                      | Why not                                                                                                                                                                                                                                             |
|---------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Keep the naive handler                                                                      | Correct on the happy path only. The orphaned-state test in the first commit of this branch shows the platform holding an article the database says is a Draft, with nothing able to repair it.                                                      |
| Hand-rolled: status table, poller, retry counter, deadline, undo list, dead-letter handling | Each piece is generic and none is about articles. Together they are a workflow engine written inside the application, without the tooling.                                                                                                          |
| Camunda                                                                                     | A process engine with BPMN as the primary artifact. The Publication is a technical flow owned by engineers, with no stakeholder review and no human tasks; the notation would carry no information. Camunda and Temporal can coexist in one system. |
| Temporal (chosen)                                                                           | Code-first workflows, retry and timeout policies declared per activity, compensation as code, an in-process test server. Fits an engineering-owned flow.                                                                                            |

The "single external call" check: one remote call by itself does not justify an engine (an output port and an
adapter are enough). The Publication has a remote step whose success the local state depends on, a retry that must
survive a crash, and two compensations that must run in order. That is the engine's job description.

## What stays in the hexagon

- Every state transition and every rule: `startPublication`, `confirmPublication` and `revertPublication` on the
  `Article` aggregate, including the ownership guard and the idempotency of the steps an activity can re-run (ADR-0003).
- Every fact: `PublicationStarted` and `ArticlePublished` are raised by the aggregate and recorded through the outbox
  inside the hexagon's own transactions (ADR-0004).
- Every step as a use case with a business name: `DistributeArticle`, `ConfirmPublication`, `RevertPublication`,
  `WithdrawArticle`, plus `RecordPublicationStarted` for the outbox handoff. The trigger keeps its Part 1 name,
  `PublishArticle`.
- No output port for the engine. The hexagon does not start the workflow; the outbox relay does (ADR-0002).

## What moves to the engine

The order of the steps, the retry policy per activity, the deadline on the remote step, and the guarantee that
compensations run in reverse order after a failure, including a worker crash. In this Publication, the workflow registers
`revertPublication` first and `withdrawArticle` second. Temporal's Saga runs them in LIFO order: `withdrawArticle`, then
`revertPublication`. It undoes the Distribution Platform effect before returning the local Article to `Draft`.

The workflow registers `withdrawArticle` before attempting `distributeArticle`. A timed-out attempt may have reached the
Distribution Platform even when the workflow did not receive its result. Withdrawal is safe if distribution never
happened, so the same compensation sequence covers both a rejected distribution and an uncertain outcome. The workflow
is a script of activity calls with no business data and no business conditionals. Each activity builds a command,
dispatches it through the command bus and classifies the `Either` it gets back: business errors become non-retryable
failures, technical errors are retried. That makes the activities a driving adapter, the same kind of component as the
REST controller and the Spring event listeners.

## Consequences

- Six use cases where Part 1 had one. All six have business names; none exists because of the engine.
- A new driving-adapter module, `workflow-input-adapter`, and a module outside the hexagon, `outbox-relay`.
- Temporal is critical infrastructure for completing a Publication. It is not needed to accept one: an author's request
  is committed locally and waits in the outbox until the engine is reachable (ADR-0002).
- Readers of the chapter should expect the extra burden (engine, relay, finer use cases, idempotent transitions) and the
  trade-off it buys: a process that converges after failure instead of one that only works when nothing fails.
