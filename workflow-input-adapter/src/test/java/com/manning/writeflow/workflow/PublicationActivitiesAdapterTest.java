package com.manning.writeflow.workflow;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.emedina.sharedkernel.command.core.CommandBus;
import com.manning.writeflow.application.command.ConfirmPublicationCommand;
import com.manning.writeflow.application.command.DistributeArticleCommand;
import com.manning.writeflow.application.command.RevertPublicationCommand;
import com.manning.writeflow.application.command.WithdrawArticleCommand;
import com.manning.writeflow.shared.error.Error;

import io.temporal.failure.ApplicationFailure;
import io.vavr.control.Either;

import static org.assertj.core.api.BDDAssertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests command dispatch and failure classification at the Temporal boundary.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
class PublicationActivitiesAdapterTest {

    private static final String ARTICLE_ID = "f3f4dd28-9f3d-4cb4-bc9a-3a7ccbc7a700";

    private CommandBus commandBus;
    private PublicationActivitiesAdapter subject;

    @BeforeEach
    void setUp() {
        this.commandBus = mock(CommandBus.class);
        this.subject = new PublicationActivitiesAdapter(this.commandBus);
    }

    @Test
    @DisplayName("distributeArticle should make business errors non-retryable")
    void distributeArticleShouldMakeBusinessErrorsNonRetryable() {
        //given
        final Error error = new Error.BusinessError.DistributionRejected(ARTICLE_ID, "rejected");
        given(this.commandBus.<Error, DistributeArticleCommand>execute(any(DistributeArticleCommand.class)))
            .willReturn(Either.left(error));

        //when
        final ApplicationFailure result = catchThrowableOfType(
            () -> this.subject.distributeArticle(ARTICLE_ID),
            ApplicationFailure.class);

        //then
        then(result.isNonRetryable()).isTrue();
        verify(this.commandBus).execute(any(DistributeArticleCommand.class));
    }

    @Test
    @DisplayName("distributeArticle should make validation errors non-retryable")
    void distributeArticleShouldMakeValidationErrorsNonRetryable() {
        //given
        //when
        final ApplicationFailure result = catchThrowableOfType(
            () -> this.subject.distributeArticle("invalid"),
            ApplicationFailure.class);

        //then
        then(result.isNonRetryable()).isTrue();
        verifyNoInteractions(this.commandBus);
    }

    @Test
    @DisplayName("confirmPublication should make multiple errors non-retryable and supply an instant")
    void confirmPublicationShouldMakeMultipleErrorsNonRetryableAndSupplyAnInstant() {
        //given
        final Error error = new Error.MultipleErrors(List.of(
            new Error.BusinessError.PublicationInProgress(ARTICLE_ID)));
        given(this.commandBus.<Error, ConfirmPublicationCommand>execute(any(ConfirmPublicationCommand.class)))
            .willReturn(Either.left(error));

        //when
        final ApplicationFailure result = catchThrowableOfType(
            () -> this.subject.confirmPublication(ARTICLE_ID),
            ApplicationFailure.class);

        //then
        then(result.isNonRetryable()).isTrue();
        final ArgumentCaptor<ConfirmPublicationCommand> command = ArgumentCaptor.forClass(ConfirmPublicationCommand.class);
        verify(this.commandBus).execute(command.capture());
        then(command.getValue().publishedAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    @DisplayName("revertPublication should leave technical errors retryable")
    void revertPublicationShouldLeaveTechnicalErrorsRetryable() {
        //given
        final Error error = new Error.TechnicalError.SomethingWentWrong("adapter", "temporary");
        given(this.commandBus.<Error, RevertPublicationCommand>execute(any(RevertPublicationCommand.class)))
            .willReturn(Either.left(error));

        //when
        final ApplicationFailure result = catchThrowableOfType(
            () -> this.subject.revertPublication(ARTICLE_ID),
            ApplicationFailure.class);

        //then
        then(result.isNonRetryable()).isFalse();
    }

    @Test
    @DisplayName("withdrawArticle should dispatch a validated command")
    void withdrawArticleShouldDispatchAValidatedCommand() {
        //given
        given(this.commandBus.<Error, WithdrawArticleCommand>execute(any(WithdrawArticleCommand.class)))
            .willReturn(Either.right(null));

        //when
        this.subject.withdrawArticle(ARTICLE_ID);

        //then
        verify(this.commandBus).execute(any(WithdrawArticleCommand.class));
    }
}
