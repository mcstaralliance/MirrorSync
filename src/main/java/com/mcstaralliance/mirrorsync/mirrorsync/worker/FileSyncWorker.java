package com.mcstaralliance.mirrorsync.mirrorsync.worker;

import com.mcstaralliance.mirrorsync.mirrorsync.model.RemoteRegistryEntry;
import com.mcstaralliance.mirrorsync.mirrorsync.service.FileSystemService;
import com.mcstaralliance.mirrorsync.mirrorsync.service.NetworkService;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

public class FileSyncWorker implements Runnable {

    private final FileSystemService fileSystemService;

    private final Logger logger = LogUtils.getLogger();
    private final RemoteRegistryEntry registryEntry;
    private final Path workPath;
    private final NetworkService networkService;
    private final Supplier<MessageDigest> digest;

    public FileSyncWorker(FileSystemService fileSystemService, RemoteRegistryEntry registryEntry, Path workPath, NetworkService networkService, Supplier<MessageDigest> digest) {
        this.fileSystemService = fileSystemService;
        this.registryEntry = registryEntry;
        this.workPath = workPath;
        this.networkService = networkService;
        this.digest = digest;
    }


    private static void ensureParentDirExists(Path path) throws IOException {
        Files.createDirectories(path.getParent());
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

            ensureParentDirExists(localFilePath);

            networkService.downloadFile(URI.create(registryEntry.downloadUrl()), localFilePath, fileSystemService);

            logger.info("[MirrorSync] Downloaded registry entry ({}) to local ({}) from remote ({})", registryEntry.filename(), localFilePath, registryEntry.downloadUrl());


            String actualHash = checkSumOf(localFilePath).toLowerCase();
            if (!actualHash.equalsIgnoreCase(registryEntry.hash())) {
                String message = String.format("Registry entry (%s) check sum failed, expected %s, actual %s", registryEntry.filename(), registryEntry.hash(), actualHash);
                logger.warn("[MirrorSync] " + message);
                throw new IOException(message);
            }

            logger.info("[MirrorSync] Synced registry entry ({}) from remote ({}) to local ({})", registryEntry.filename(), registryEntry.downloadUrl(), localFilePath);
        } catch (IOException e) {
            throw new CompletionException(String.format("[MirrorSync] Sync registry entry (%s) failed", registryEntry.filename()), e);
        }
    }


    private String checkSumOf(Path local) throws IOException {
        MessageDigest digest = this.digest.get();
        digest.reset();
        digest.update(fileSystemService.readBytesFrom(local));
        return HexFormat.of().formatHex(digest.digest());
    }

    private boolean shouldDownload(Path local) throws IOException {
        if (!fileSystemService.isFileReadable(local)) {
            return true;
        }
        return !registryEntry.hash().equalsIgnoreCase(checkSumOf(local));
    }
}
