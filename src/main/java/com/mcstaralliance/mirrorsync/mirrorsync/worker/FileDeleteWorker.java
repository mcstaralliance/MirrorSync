package com.mcstaralliance.mirrorsync.mirrorsync.worker;

import com.mcstaralliance.mirrorsync.mirrorsync.model.RemoteUpdateEntry;
import com.mcstaralliance.mirrorsync.mirrorsync.service.FileSystemService;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class FileDeleteWorker implements Runnable {

    private final FileSystemService fileSystemService;

    private final RemoteUpdateEntry entry;
    private final Path minecraftPath;
    private static final Logger logger = LogUtils.getLogger();

    public FileDeleteWorker(FileSystemService fileSystemService, RemoteUpdateEntry entry, Path minecraftPath) {
        this.fileSystemService = fileSystemService;
        this.entry = entry;
        this.minecraftPath = minecraftPath;
    }

    @Override
    public void run() {
        Path absoluteDirPath = minecraftPath.resolve(entry.dirPath());
        try (Stream<Path> files = Files.list(absoluteDirPath)) {
            List<Path> filesToDelete = files.filter(file -> !entry.children().contains(file.getFileName().toString()))
                    .toList();
            logger.info("[MirrorSync] Files will be deleted: {}", filesToDelete);

            for (Path path : filesToDelete) {
                if (fileSystemService.deleteFile(path)) {
                    logger.info("[MirrorSync] Deleted file: {}", path);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
