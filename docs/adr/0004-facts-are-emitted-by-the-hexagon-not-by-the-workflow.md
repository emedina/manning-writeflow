Status: accepted

# Facts are emitted by the hexagon, not by the workflow

Integration events are raised by the `Article` aggregate and recorded in the outbox inside the transaction of the step
that makes them true. The workflow emits nothing. `ArticlePublished` is raised by `confirmPublication`, the last step of
the saga; `PublicationStarted` is raised by `startPublication`, in the trigger handler's transaction.

## The rule

A fact belongs to the step that makes it true. If the fact asserts that the whole process is done, it belongs to the
last step, inside that step's transaction. Anything emitted earlier is a different fact with a weaker name, and its
consumers may infer only what the name states.

| Fact                 | Raised by                                               | Asserts                                                                   | Consumers                                                                                            |
|----------------------|---------------------------------------------------------|---------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------|
| `PublicationStarted` | `startPublication`, trigger transaction                 | An author requested publication; the article is `Publishing`.             | The outbox relay, which starts the workflow. No other consumer may read it as "published".           |
| `ArticlePublished`   | `confirmPublication`, confirming activity's transaction | The article is `Published` here and present on the Distribution Platform. | The Part 1 consumers: the outbox row for other contexts, the `AFTER_COMMIT` subscriber notification. |

## Why the confirming step is the right place

`confirmPublication` is the saga's point of no return. The saga runs it only after `distribute` succeeded, and registers
no compensation after it. At the moment its transaction commits, the article is `Published` locally, the platform has
it, and nothing the consumers could need is still in flight. Workflow completion is engine bookkeeping that happens
after that commit; no consumer waits for it.

The crash case: the confirming activity commits the status change and the outbox row, the worker dies before reporting,
Temporal runs the activity again. `confirmPublication` on a `Published` article returns right with no event (ADR-0003),
so no second row is written and no consumer sees the fact twice.

## Considered options

| Option                                                                    | Why not                                                                                                                                                                                                     |
|---------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Raise `ArticlePublished` from the trigger handler, where Part 1 raised it | Part 1 could, because the handler completed the whole publication. Now the trigger only starts it. Consumers would index or announce an article the platform may still reject.                              |
| Add an activity after `confirmPublication` whose job is to emit the fact  | The fact must be atomic with the state change, which is what the outbox gives us. An activity that runs after the commit is a second write to a second system: the dual write this branch exists to remove. |
| Let the workflow publish the fact when it completes                       | Same dual write, and the workflow would hold business data it has no other reason to know.                                                                                                                  |

## Consequences

- The Part 1 pipeline for `ArticlePublished` (domain event, `BEFORE_COMMIT` listener, record command and handler,
  outbox adapter) is unchanged in shape and moves from the publish handler's transaction to the confirming handler's.
  `PublicationStarted` gets a sibling pipeline of the same shape rather than a generic one, so every Part 1 class stays
  recognisable; a generic, sealed-event pipeline is a possible later refactor.
- Both `BEFORE_COMMIT` listeners throw on a left so the commit aborts. The Part 1 listener only logged the left, which
  meant a `Published` article could commit without its outbox row; this branch fixes it.
- Data produced by an earlier step that a consumer needs, such as the platform's distribution reference, flows forward
  through the hexagon: the activity returns it, the workflow passes the id, the confirming command carries it, the
  aggregate records it, the fact carries it. The reference application does not add that field; the path is the point.
- The workflow registers compensations in forward-dependency order: `revertPublication`, then `withdrawArticle`.
  Temporal's Saga executes them in reverse order, so a failure withdraws from the Distribution Platform before returning
  the Article to `Draft`. `withdrawArticle` is registered before `distribute` because a timed-out attempt may have
  succeeded without its result reaching the workflow. It must also succeed when the Article was never distributed.
- Timeouts never trigger compensation of the confirming step: the overall deadline exists only on `distribute`;
  `confirm` and the compensations retry without an attempt limit, so a timed-out attempt re-runs and meets the idempotent
  transition.
