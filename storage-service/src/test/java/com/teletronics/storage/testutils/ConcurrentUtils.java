package com.teletronics.storage.testutils;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import lombok.experimental.UtilityClass;

import static org.awaitility.Awaitility.await;

/**
 * Helpers for tests that run code concurrently or depend on the clock.
 */
@UtilityClass
public class ConcurrentUtils {
    /**
     * Runs the task in {@code count} threads that all start at the same moment,
     * to provoke races, and collects the results.
     *
     * @param count number of threads, and of task runs
     * @param task  the task to run in every thread
     * @param <T>   result type of the task
     * @return the results of all runs, in thread submission order
     * @throws Exception if a run fails, or does not finish within 30 seconds
     */
    public static <T> List<T> runConcurrently(int count, Callable<T> task) throws Exception {
        try (ExecutorService executor = Executors.newFixedThreadPool(count)) {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        }
    }

    /**
     * Waits until the clock, at millisecond precision, is strictly after the given moment.
     * Guarantees that the next {@code Instant.now()} differs from it once truncated to milliseconds,
     * as MongoDB stores dates with that precision. Gives up after one second.
     *
     * @param moment the moment the clock has to pass
     */
    public static void awaitClockAfter(Instant moment) {
        await().pollDelay(Duration.ZERO)
                .pollInterval(Duration.ofMillis(1))
                .atMost(Duration.ofSeconds(1))
                .until(() -> Instant.now().truncatedTo(ChronoUnit.MILLIS)
                        .isAfter(moment.truncatedTo(ChronoUnit.MILLIS)));
    }
}
