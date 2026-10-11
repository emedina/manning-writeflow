package com.manning.writeflow.application.ports.out;

import com.emedina.sharedkernel.application.annotation.OutputPort;
import com.manning.writeflow.shared.dto.ArticleForDistributionDTO;
import com.manning.writeflow.shared.error.Error;
import io.vavr.control.Either;

/**
 * Represents the application's distribution capability.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
@OutputPort
public interface DistributionPlatformOutputPort {

    /**
     * Hands an article to the Distribution Platform, idempotently by article id.
     *
     * @param article the article to distribute
     * @return success or an error when distribution is rejected
     */
    Either<Error, Void> distribute(final ArticleForDistributionDTO article);

    /**
     * Withdraws an article from the Distribution Platform, idempotently by article id.
     * This operation has no business failures.
     *
     * @param articleId the identifier of the article to withdraw
     * @return success or a technical error
     */
    Either<Error, Void> withdraw(final String articleId);

}
