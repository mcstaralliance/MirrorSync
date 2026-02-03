package com.mcstaralliance.mirrorsync.mirrorsync.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;

public interface FileSystemService {
    void saveBytesTo(Path path, InputStream inputStream) throws IOException;

    ByteBuffer readBytesFrom(Path path) throws IOException;

    boolean isFileReadable(Path path);

    boolean deleteFile(Path path);
}
