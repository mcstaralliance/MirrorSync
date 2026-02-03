package com.mcstaralliance.mirrorsync.mirrorsync.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class StandardFileSystemService implements FileSystemService {
    @Override
    public void saveBytesTo(Path path, InputStream inputStream, long length) throws IOException {
        try (ReadableByteChannel src = Channels.newChannel(inputStream);
             FileChannel dest = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
             dest.transferFrom(src, 0, length <= 0 ? Long.MAX_VALUE : length);
        }
    }

    @Override
    public ByteBuffer readBytesFrom(Path path) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            return channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        }
    }

    @Override
    public boolean isFileReadable(Path path) {
        return Files.exists(path) && Files.isReadable(path);
    }

    @Override
    public boolean deleteFile(Path path) {
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            return false;
        }
    }
}