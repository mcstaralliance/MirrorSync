package com.mcstaralliance.mirrorsync.mirrorsync.worker;

import com.mcstaralliance.mirrorsync.mirrorsync.model.RemoteRegistryEntry;
import com.mcstaralliance.mirrorsync.mirrorsync.service.NetworkService;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.function.Supplier;

public class FileSyncWorker implements Runnable {

    private final Logger logger = LogUtils.getLogger();
    private final RemoteRegistryEntry registryEntry;
    private final Path workPath;
    private final NetworkService networkService;
    private final Supplier<MessageDigest> digest;
    private byte[] remoteDigest;

    public FileSyncWorker(RemoteRegistryEntry registryEntry, Path workPath, NetworkService networkService, Supplier<MessageDigest> digest) {
        this.registryEntry = registryEntry;
        this.workPath = workPath;
        this.networkService = networkService;
        this.digest = digest;
    }

    private byte[] getRemoteDigest() {
        if (remoteDigest == null) {
            remoteDigest = HexFormat.of().parseHex(registryEntry.hash());
        }
        return remoteDigest;
    }

    private static void ensureExists(Path path) throws IOException {
        if (Files.exists(path)) {
            return;
        }
        Files.createDirectories(path.getParent());
        Files.createFile(path);
    }

    @Override
    public void run() {

        Path savePath = Path.of(registryEntry.savePath());

        if (savePath.startsWith(".minecraft")) {
            savePath = savePath.subpath(1, savePath.getNameCount());
        }

        Path localFilePath = workPath.resolve(savePath);

        try {
            if (!shouldDownload(localFilePath)) {
                logger.info("[MirrorSync] Registry entry ({}) with local ({}) is up to date!", registryEntry.filename(), localFilePath);
                return;
            }

            logger.info("[MirrorSync] Start syncing registry entry ({})", registryEntry.filename());

            ensureExists(localFilePath);

            networkService.downloadFile(URI.create(registryEntry.downloadUrl()), localFilePath);

            logger.info("[MirrorSync] Downloaded registry entry ({}) to local ({}) from remote ({})", registryEntry.filename(), localFilePath, registryEntry.downloadUrl());

            if (!Arrays.equals(checkSumOf(localFilePath), getRemoteDigest())) {
                String message = String.format("[MirrorSync] Registry entry (%s) check sum failed", registryEntry.filename());
                logger.warn(message);
                throw new RuntimeException(message);
            }
            logger.info("[MirrorSync] Synced registry entry ({}) from remote ({}) to local ({})", registryEntry.filename(), registryEntry.downloadUrl(), localFilePath);
        } catch (IOException e) {
            throw new RuntimeException(String.format("[MirrorSync] Sync registry entry (%s) failed", registryEntry.filename()), e);
        }
    }


    private byte[] checkSumOf(Path local) throws IOException {
        MessageDigest digest = this.digest.get();
        digest.reset();
        try (var inputStream = Files.newInputStream(local, StandardOpenOption.READ)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return digest.digest();
        }
    }

    private boolean shouldDownload(Path local) throws IOException {
        if (!Files.isReadable(local)) {
            return true;
        }
        return !Arrays.equals(checkSumOf(local), getRemoteDigest());
    }
}
