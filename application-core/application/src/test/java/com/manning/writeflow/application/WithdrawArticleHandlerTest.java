package com.manning.writeflow.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.manning.writeflow.application.command.WithdrawArticleCommand;
import com.manning.writeflow.application.ports.out.DistributionPlatformOutputPort;
import com.manning.writeflow.shared.error.Error;
import io.vavr.control.Either;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

/**
 * Tests for {@link WithdrawArticleHandler}.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
@ExtendWith(MockitoExtension.class)
class WithdrawArticleHandlerTest {

    private static final String ARTICLE_ID = "11111111-1111-1111-1111-111111111111";
    @Mock private DistributionPlatformOutputPort distributionPlatform;
    @InjectMocks private WithdrawArticleHandler handler;

    @Test
    @DisplayName("handle should complete the use case")
    void handleShouldCompleteUseCase() {
        //given
        given(this.distributionPlatform.withdraw(ARTICLE_ID)).willReturn(Either.right(null));

        //when
        final Either<Error, Void> result = this.handler.handle(WithdrawArticleCommand.validateThenCreate(ARTICLE_ID).get());

        //then
        then(result.isRight()).isTrue();
        verify(this.distributionPlatform).withdraw(ARTICLE_ID);
    }

    @Test
    @DisplayName("handle should stop after an upstream failure")
    void handleShouldStopAfterUpstreamFailure() {
        //given
        final Error.TechnicalError.SomethingWentWrong error = new Error.TechnicalError.SomethingWentWrong("platform", "failed");
        given(this.distributionPlatform.withdraw(ARTICLE_ID)).willReturn(Either.left(error));

        //when
        final Either<Error, Void> result = this.handler.handle(WithdrawArticleCommand.validateThenCreate(ARTICLE_ID).get());

        //then
        then(result.getLeft()).isEqualTo(error);
        verify(this.distributionPlatform).withdraw(ARTICLE_ID);
    }
}
