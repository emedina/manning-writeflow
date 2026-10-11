package com.manning.writeflow.workflow;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.temporal.worker.WorkerFactory;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests Temporal worker startup and shutdown without a Spring context.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
class TemporalWorkerStartupTest {

    private WorkerFactory workerFactory;
    private TemporalWorkerStartup subject;

    @BeforeEach
    void setUp() {
        this.workerFactory = mock(WorkerFactory.class);
        this.subject = new TemporalWorkerStartup(this.workerFactory);
    }

    @Test
    @DisplayName("start should start workers asynchronously")
    void startShouldStartWorkersAsynchronously() throws InterruptedException {
        //given
        final CountDownLatch started = new CountDownLatch(1);
        willAnswer(invocation -> {
            started.countDown();
            return null;
        }).given(this.workerFactory).start();

        //when
        this.subject.start();

        //then
        then(this.subject.isRunning()).isTrue();
        then(started.await(2, TimeUnit.SECONDS)).isTrue();
        verify(this.workerFactory, times(1)).start();
        this.subject.stop();
    }

    @Test
    @DisplayName("start should retry after a transient worker startup failure")
    void startShouldRetryAfterATransientWorkerStartupFailure() throws InterruptedException {
        //given
        final AtomicInteger attempts = new AtomicInteger();
        final CountDownLatch started = new CountDownLatch(1);
        willAnswer(invocation -> {
            if (attempts.getAndIncrement() == 0) {
                throw new IllegalStateException("Temporal is unavailable");
            }
            started.countDown();
            return null;
        }).given(this.workerFactory).start();

        //when
        this.subject.start();

        //then
        then(started.await(5, TimeUnit.SECONDS)).isTrue();
        then(attempts.get()).isEqualTo(2);
        verify(this.workerFactory, times(2)).start();
        this.subject.stop();
    }

    @Test
    @DisplayName("stop should interrupt a retry wait")
    void stopShouldInterruptARetryWait() throws InterruptedException {
        //given
        final CountDownLatch failedStart = new CountDownLatch(1);
        final CountDownLatch waitingForRetry = new CountDownLatch(1);
        final CountDownLatch interrupted = new CountDownLatch(1);
        final AtomicReference<Duration> retryDelay = new AtomicReference<>();
        final TemporalWorkerStartup.RetrySleeper sleeper = (final Duration delay) -> {
            retryDelay.set(delay);
            waitingForRetry.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (final InterruptedException exception) {
                interrupted.countDown();
                throw exception;
            }
        };
        this.subject = new TemporalWorkerStartup(this.workerFactory, sleeper);
        willAnswer(invocation -> {
            failedStart.countDown();
            throw new IllegalStateException("Temporal is unavailable");
        }).given(this.workerFactory).start();

        //when
        this.subject.start();
        final boolean didStart = failedStart.await(2, TimeUnit.SECONDS);
        final boolean didWait = waitingForRetry.await(2, TimeUnit.SECONDS);
        this.subject.stop();

        //then
        then(didStart).isTrue();
        then(didWait).isTrue();
        then(interrupted.await(2, TimeUnit.SECONDS)).isTrue();
        then(retryDelay.get()).isEqualTo(Duration.ofSeconds(1));
        then(this.subject.isRunning()).isFalse();
        verify(this.workerFactory, times(1)).start();
    }

    @Test
    @DisplayName("stop callback should run and lifecycle should report its startup policy")
    void stopCallbackShouldRunAndLifecycleShouldReportItsStartupPolicy() {
        //given
        final AtomicInteger callbacks = new AtomicInteger();

        //when
        this.subject.stop(callbacks::incrementAndGet);

        //then
        then(this.subject.isAutoStartup()).isTrue();
        then(this.subject.isRunning()).isFalse();
        then(callbacks.get()).isEqualTo(1);
    }
}
