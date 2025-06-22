package com.mcstaralliance.mirrorsync.mirrorsync.service;

import com.mcstaralliance.mirrorsync.mirrorsync.model.RemoteUpdateEntry;
import com.mcstaralliance.mirrorsync.mirrorsync.util.WithRetry;
import com.mcstaralliance.mirrorsync.mirrorsync.model.RemoteRegistryEntry;
import com.mcstaralliance.mirrorsync.mirrorsync.worker.FileDeleteWorker;
import com.mcstaralliance.mirrorsync.mirrorsync.worker.FileSyncWorker;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.*;

public class FileSyncService {

    private FileSyncService() {}

    private static final FileSyncService INSTANCE = new FileSyncService();

    private final Logger logger = LogUtils.getLogger();

    public static FileSyncService getInstance() {
        return INSTANCE;
    }

    private final ThreadLocal<MessageDigest> messageDigest = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    });

    public void sync(Path minecraftPath) {

        ExecutorService executorService = null;

        try (NetworkService networkService = new NetworkService()) {
            logger.info("Checking whether we need to do sync files...");

            boolean shouldCheck;

            try {
                shouldCheck = networkService.shouldCheck();
            } catch (IOException e) {
                throw new RuntimeException("Check sync flag failed", e);
            }

            logger.info("Sync flag: {}", shouldCheck);

            if (!shouldCheck) return;

            logger.info("Minecraft path: {}", minecraftPath);

            List<RemoteRegistryEntry> remoteRegistryEntries = networkService.retrieveRemoteRegistryEntries();

            logger.info("Retried remote registry entries: {}", remoteRegistryEntries);

            executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

            // To make compiler happy
            final ExecutorService finalExecutorService = executorService;

            @SuppressWarnings("rawtypes")
            CompletableFuture[] completableFutures = remoteRegistryEntries.stream()
                    .map(entry -> WithRetry.withRetry(new FileSyncWorker(entry, minecraftPath, networkService, messageDigest::get), 3, finalExecutorService))
                    .toArray(CompletableFuture[]::new);

            CompletableFuture.allOf(completableFutures).join();

            List<RemoteUpdateEntry> remoteUpdateEntries = networkService.retrieveRemoteUpdateEntries();

            logger.info("Retried remote update entries: {}", remoteUpdateEntries);

            completableFutures = remoteUpdateEntries.stream()
                    .map(entry -> WithRetry.withRetry(new FileDeleteWorker(entry, minecraftPath), 3, finalExecutorService))
                    .toArray(CompletableFuture[]::new);

            CompletableFuture.allOf(completableFutures).join();
            
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (executorService != null) {
                executorService.shutdownNow();
                logger.info("Shutdown sync executor service");
            }
        }
    }
}
