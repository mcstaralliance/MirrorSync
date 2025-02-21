package com.mcstaralliance.mirrorsync.mirrorsync.util;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

public class WithRetry<T> {

    private final Callable<T> task;
    private final int maxRetries;
    private final ExecutorService executor;
    private int currentAttempt;

    private WithRetry(Callable<T> task, int maxRetries, ExecutorService executor) {
        this.task = task;
        this.maxRetries = maxRetries;
        this.executor = executor;
        this.currentAttempt = 0;
    }

    public static CompletableFuture<Void> withRetry(Runnable task, int maxRetries, ExecutorService executor) {
        return withRetry(() -> {
            task.run();
            return null;
        }, maxRetries, executor);
    }


    public static <T> CompletableFuture<T> withRetry(
            Callable<T> task,
            int maxRetries,
            ExecutorService executor
    ) {
        WithRetry<T> wrappedTask = new WithRetry<>(task, maxRetries, executor);
        return wrappedTask.attempt();
    }

    private CompletableFuture<T> attempt() {
        return CompletableFuture.supplyAsync(() -> {
                    try {
                        currentAttempt++;
                        return task.call();
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }, executor)
                .handle((result, ex) -> {
                    if (ex == null) {
                        return CompletableFuture.completedFuture(result);
                    }

                    if (currentAttempt >= maxRetries) {
                        throw new CompletionException("Max retries reached", ex);
                    }

                    return attempt();
                })
                .thenCompose(Function.identity());
    }
}