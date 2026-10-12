package com.manning.writeflow.external.distribution;

import com.emedina.sharedkernel.application.annotation.Adapter;
import com.manning.writeflow.application.ports.out.DistributionPlatformOutputPort;
import com.manning.writeflow.shared.dto.ArticleForDistributionDTO;
import com.manning.writeflow.shared.error.Error;
import io.vavr.control.Either;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Fake Distribution Platform that logs calls and remembers distributed article identifiers.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
@Slf4j
@Adapter
final class FakeDistributionPlatformAdapter implements DistributionPlatformOutputPort {

    private static final String REJECT_NEXT_PROPERTY = "writeflow.fake-distribution.reject-next";

    private final Set<String> distributedArticleIds = ConcurrentHashMap.newKeySet();
    private final AtomicReference<Error> nextFailure = new AtomicReference<>();
    private final AtomicBoolean rejectNextDistribution = new AtomicBoolean(Boolean.getBoolean(REJECT_NEXT_PROPERTY));

    /**
     * Configures the next distribution call to return the supplied failure.
     *
     * @param error the failure to return
     */
    void failNextWith(final Error error) {
        this.nextFailure.set(Objects.requireNonNull(error, "error must not be null"));
    }

    boolean hasDistributed(final String articleId) {
        return this.distributedArticleIds.contains(articleId);
    }

    @Override
    public Either<Error, Void> distribute(final ArticleForDistributionDTO article) {
        final Error failure = this.nextFailureFor(article);
        if (Objects.nonNull(failure)) {
            log.info("Distribution Platform rejected article {}", article.id());
            return Either.left(failure);
        }

        this.distributedArticleIds.add(article.id());
        log.info("Pretend Distribution Platform accepted article {}", article.id());
        return Either.right(null);
    }

    private Error nextFailureFor(final ArticleForDistributionDTO article) {
        final Error failure = this.nextFailure.getAndSet(null);
        if (Objects.nonNull(failure)) {
            return failure;
        }
        return this.rejectNextDistribution.compareAndSet(true, false)
            ? new Error.BusinessError.DistributionRejected(article.id(), "Rejected by demo configuration")
            : null;
    }

    @Override
    public Either<Error, Void> withdraw(final String articleId) {
        this.distributedArticleIds.remove(articleId);
        log.info("Pretend Distribution Platform withdrew article {}", articleId);
        return Either.right(null);
    }

}
