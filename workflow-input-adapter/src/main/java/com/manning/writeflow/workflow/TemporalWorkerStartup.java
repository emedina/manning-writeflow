package com.manning.writeflow.workflow;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import io.temporal.worker.WorkerFactory;

/**
 * Starts Temporal workers without making application startup depend on the Temporal service.
 *
 * @author Enrique Medina Montenegro (emedina)
 */
final class TemporalWorkerStartup implements SmartLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemporalWorkerStartup.class);
    private static final Duration INITIAL_RETRY_DELAY = Duration.ofSeconds(1);
    private static final Duration MAXIMUM_RETRY_DELAY = Duration.ofSeconds(30);

    private final WorkerFactory workerFactory;
    private final RetrySleeper retrySleeper;
    private volatile boolean running;
    private volatile Thread startupThread;

    TemporalWorkerStartup(final WorkerFactory workerFactory) {
        this(workerFactory, Thread::sleep);
    }

    TemporalWorkerStartup(final WorkerFactory workerFactory, final RetrySleeper retrySleeper) {
        this.workerFactory = workerFactory;
        this.retrySleeper = retrySleeper;
    }

    @Override
    public void start() {
        this.running = true;
        this.startupThread = Thread.ofVirtual()
            .name("temporal-worker-startup")
            .start(this::startWorkersWhenAvailable);
    }

    @Override
    public void stop() {
        this.running = false;
        if (this.startupThread != null) {
            this.startupThread.interrupt();
        }
    }

    @Override
    public void stop(final Runnable callback) {
        this.stop();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    private void startWorkersWhenAvailable() {
        Duration retryDelay = INITIAL_RETRY_DELAY;
        while (this.running) {
            try {
                this.workerFactory.start();
                LOGGER.info("Temporal workers started");
                return;
            } catch (final RuntimeException exception) {
                LOGGER.warn("Temporal worker startup failed; retrying after {}", retryDelay, exception);
                try {
                    this.retrySleeper.sleep(retryDelay);
                } catch (final InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return;
                }
                retryDelay = retryDelay.multipliedBy(2).compareTo(MAXIMUM_RETRY_DELAY) > 0
                    ? MAXIMUM_RETRY_DELAY
                    : retryDelay.multipliedBy(2);
            }
        }
    }

    @FunctionalInterface
    interface RetrySleeper {

        void sleep(final Duration delay) throws InterruptedException;
    }

}
