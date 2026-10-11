Status: accepted

# Trigger the workflow through the outbox

The Publication workflow is started by a relay that reads the transactional outbox, not by the handler that accepts
the author's request. `PublishArticleHandler` keeps its Part 1 shape: one local transaction that moves the article to
`Publishing`, raises `PublicationStarted`, and (through the `BEFORE_COMMIT` listener) writes that fact to the outbox.
The relay reads the row and starts the workflow. The hexagon has no output port for the engine and never learns that
one exists.

## Considered options

| Option                                                                      | Failure                                                                                                                                                                                                                                                                                                                                  |
|-----------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Handler writes `Publishing` and calls the engine client in the same method  | A dual write, one level up from the one we are fixing. A crash after the commit and before the start leaves an article in `Publishing` that no workflow will ever pick up; a start before the commit can orphan a workflow for an article that stays `Draft`.                                                                            |
| Handler writes `Publishing`; an `AFTER_COMMIT` listener starts the workflow | `AFTER_COMMIT` runs in process and is not durable. A crash after the commit loses the start, with the same symptom as above.                                                                                                                                                                                                             |
| Handler does no local write; starting the workflow is its only effect       | Durable, and Temporal's idiomatic shape. Two costs. The engine owns the fact "a publication was requested": a second `POST` gets a 409 from the engine while `GET` still says `Draft`, so two components answer the same question differently. And publishing is tied to the engine's availability: engine down, authors cannot publish. |
| Outbox row, read by a relay that starts the workflow (chosen)               | The aggregate owns the fact and issues the 409; `GET` agrees at once. Engine down: the request still commits and the row waits. One more component, the relay, which Part 1 had deferred.                                                                                                                                                |

The outbox is already the hexagon's bridge from a local transaction to the outside; starting an orchestrator is one
more external effect that crosses it. The relay's at-least-once "start, then mark" is safe because the workflow id is
derived from the outbox row (`publication-{articleId}-{eventId}`) with the reject-duplicate reuse policy: a replayed row
gets "already started", which the relay treats as success. A later, legitimate publication of the same article (after a
revert) has a new event id and so a new workflow.

## The relay is not an adapter

The relay implements no input port and no output port. It reads a table and calls an engine client. By the definition
Part 1 uses for adapters, it is not one, and "start the workflow" is not a use case. Its module is named `outbox-relay`,
without the `-adapter` suffix, and an ArchUnit rule asserts that its package depends on no other package of the
application. It starts the workflow through an untyped stub by workflow type name, so it does not depend on the
`workflow-input-adapter` module either. A contract test in the assembly checks that the type name and the payload
fields the relay reads match the workflow interface and the integration-event record.

## REST contract

`POST /api/articles/{id}/publication` returns `202 Accepted` with a `Location` header for the article (Part 1 returned
`204`). The request was accepted; the article shows `PUBLISHING` until the workflow confirms it. The request body no
longer carries a publication date; the confirming step stamps the instant.

## Simplifications in this reference application

Each of these is right for a reference application and should not be copied into production as is:

- One outbox table carries both the engine handoff (`PublicationStarted`) and the integration event
  (`ArticlePublished`). Production systems often separate the two or use change-data capture.
- The relay is a scheduled poller inside the same deployable, single instance, with no locking across instances.
- `ArticlePublished` rows are "delivered" by writing a log line. A production relay publishes them to a broker.
- `PublicationStarted` means "an author requested publication". No consumer may read it as "published".

## Consequences

- The hexagon keeps its truth and its availability. Temporal is needed to complete a Publication, not to accept one.
- A second `PublicationStarted` row for one request cannot exist: `startPublication` on a `Publishing` article returns a
  business error, so the trigger handler never raises the event twice (ADR-0003).
- The author's hexagonal reference flow starts the process engine inside a transactional handler, which is the first
  rejected option above. The reference should be aligned with this decision.
