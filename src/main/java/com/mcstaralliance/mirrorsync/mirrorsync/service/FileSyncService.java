package com.mcstaralliance.mirrorsync.mirrorsync.service;

import com.mcstaralliance.mirrorsync.mirrorsync.model.RemoteUpdateEntry;
import com.mcstaralliance.mirrorsync.mirrorsync.util.WithRetry;
import com.mcstaralliance.mirrorsync.mirrorsync.model.RemoteRegistryEntry;
import com.mcstaralliance.mirrorsync.mirrorsync.worker.FileDeleteWorker;
import com.mcstaralliance.mirrorsync.mirrorsync.worker.FileSyncWorker;
import com.mojang.logging.LogUtils;
import com.sun.jna.Platform;
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

        FileSystemService fileSystemService = Platform.isWindows() ? new WindowsFileSystemService() : new StandardFileSystemService();

        ExecutorService executorService = null;

        try (NetworkService networkService = new NetworkService()) {
            logger.info("[MirrorSync] Checking whether we need to do sync files...");

            boolean shouldCheck;

            try {
                shouldCheck = networkService.shouldCheck();
            } catch (IOException e) {
                throw new RuntimeException("Check sync flag failed", e);
            }

            logger.info("[MirrorSync] Sync flag: {}", shouldCheck);

            if (!shouldCheck) return;

            logger.info("[MirrorSync] Minecraft path: {}", minecraftPath);

            List<RemoteRegistryEntry> remoteRegistryEntries = networkService.retrieveRemoteRegistryEntries();

            logger.info("[MirrorSync] Retried remote registry entries: {}", remoteRegistryEntries);

//            executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            executorService = Executors.newFixedThreadPool(16);

            // To make compiler happy
            final ExecutorService finalExecutorService = executorService;

            @SuppressWarnings("rawtypes")
            CompletableFuture[] completableFutures = remoteRegistryEntries.stream()
                    .map(entry -> WithRetry.withRetry(new FileSyncWorker(fileSystemService, entry, minecraftPath, networkService, messageDigest::get), 3, finalExecutorService))
                    .toArray(CompletableFuture[]::new);

            CompletableFuture.allOf(completableFutures).join();

            List<RemoteUpdateEntry> remoteUpdateEntries = networkService.retrieveRemoteUpdateEntries();

            logger.info("[Mirror Sync] Retried remote update entries: {}", remoteUpdateEntries);

            completableFutures = remoteUpdateEntries.stream()
                    .map(entry -> WithRetry.withRetry(new FileDeleteWorker(fileSystemService, entry, minecraftPath), 3, finalExecutorService))
                    .toArray(CompletableFuture[]::new);

            CompletableFuture.allOf(completableFutures).join();
            
        } catch (Exception e) {
            logger.error("[MirrorSync] Error in sync files, ", e);
        } finally {
            if (executorService != null) {
                executorService.shutdownNow();
                logger.info("[MirrorSync] Shutdown sync executor service");
            }
        }
    }
}
