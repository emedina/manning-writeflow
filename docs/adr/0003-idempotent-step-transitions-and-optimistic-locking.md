Status: accepted

# Idempotent step transitions and optimistic locking on the Article

Temporal runs activities at least once: an activity that committed its work and died before reporting is run again.
The step transitions that an activity can reach, `confirmPublication` and `revertPublication`, are therefore idempotent
on the `Article` aggregate, and `JpaArticle` carries a version attribute for optimistic locking. The engine guarantees
the retry; the domain and the persistence adapter make the retry safe.

## The two failures this prevents

**Without idempotent transitions.** The confirming activity commits `Publishing → Published` and writes the
`ArticlePublished` outbox row. The worker dies before reporting completion. Temporal runs the activity again. If
`confirmPublication` on a `Published` article returned `ArticleAlreadyPublished`, the activity would classify it as a
business failure, the saga would compensate, and a correctly published article would be withdrawn from the
Distribution Platform by its own retry.

**Without optimistic locking.** Attempt A of an activity stalls past its timeout. Attempt B commits. Distribution fails,
the saga reverts the article to `Draft`. Attempt A's stale transaction then commits `Publishing` over the reverted row.
The article is stuck in `Publishing` with no workflow working on it. Idempotent transitions do not detect that the row
moved on underneath a stale write; optimistic locking rejects the stale write but turns an honest duplicate into an
error unless the transition tolerates it. Both are needed.

## The transitions

| Method                            | From Publishing                        | From Published            | From Draft                                |
|-----------------------------------|----------------------------------------|---------------------------|-------------------------------------------|
| `startPublication(author)`        | `PublicationInProgress`                | `ArticleAlreadyPublished` | → Publishing, raises `PublicationStarted` |
| `confirmPublication(publishedAt)` | → Published, raises `ArticlePublished` | right, no event           | `PublicationNotStarted`                   |
| `revertPublication()`             | → Draft, no event                      | `ArticleAlreadyPublished` | right, no event                           |

Only the transitions an activity can re-run are idempotent. `startPublication` is invoked by the trigger handler, once
per request, inside its own transaction; it is not idempotent on purpose. A second request while the article is
`Publishing` is told the truth with a 409, and accepting it would be the only way a duplicate `PublicationStarted` row
could reach the relay. The ownership guard lives in `startPublication` and nowhere else.

## Where each kind of idempotency lives

- Business idempotency is an aggregate transition that returns right and raises no event. It is not an `if` in a
  handler and not a lookup in the domain.
- Technical idempotency lives in adapters: the relay keys the workflow on the outbox event id (ADR-0002), and
  `DistributionPlatformOutputPort` is idempotent by article id (a repeated `distribute` succeeds with no effect; a
  `withdraw` of an article that was never distributed succeeds with no effect).

## Consequences

- An idempotent repeat raises no event, so no duplicate outbox row is written (ADR-0004).
- `JpaArticle` gains a version attribute. Saving must apply changes to the entity loaded in the same transaction; the
  Part 1 mapper builds a fresh entity, which would collide with the managed one under versioning. This behaviour is
  guided by this note and demonstrated by running the application; it is not covered by an automated test, a deliberate
  choice for a reference application.
- The persistence adapter translates an optimistic-locking failure into the existing `PersistenceError`, which the
  activity classifies as retryable. The REST adapter translates the same failure into a 409 for the loser of two
  concurrent publish requests.
- The transition matrix above is tested in plain JUnit against the aggregate, with no engine. Retry safety is a property
  of the domain, not of Temporal.
