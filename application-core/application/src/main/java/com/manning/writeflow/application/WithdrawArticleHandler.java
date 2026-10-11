package com.manning.writeflow.application;

import com.emedina.sharedkernel.application.annotation.ApplicationService;
import com.manning.writeflow.application.command.WithdrawArticleCommand;
import com.manning.writeflow.application.ports.in.WithdrawArticleUseCase;
import com.manning.writeflow.application.ports.out.DistributionPlatformOutputPort;
import com.manning.writeflow.shared.error.Error;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;

/**
 * Coordinates the withdrawArticle use case.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
@ApplicationService
@RequiredArgsConstructor
class WithdrawArticleHandler implements WithdrawArticleUseCase {

    private final DistributionPlatformOutputPort distributionPlatform;

    @Override
    public Either<Error, Void> handle(final WithdrawArticleCommand command) {
        return this.distributionPlatform.withdraw(command.articleId());
    }

}
