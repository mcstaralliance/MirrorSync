package com.mcstaralliance.mirrorsync.mirrorsync.service;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.win32.W32APIOptions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public class WindowsFileSystemService implements FileSystemService {

    private interface LocalKernel32 extends Kernel32 {
        LocalKernel32 INSTANCE = Native.load("kernel32", LocalKernel32.class, W32APIOptions.DEFAULT_OPTIONS);

        boolean GetFileSizeEx(HANDLE hFile, WinNT.LARGE_INTEGER lpFileSize);
        boolean SetFilePointerEx(HANDLE hFile, long liDistanceToMove, Pointer lpNewFilePointer, int dwMoveMethod);
        boolean SetEndOfFile(HANDLE hFile);
        boolean FlushViewOfFile(Pointer lpBaseAddress, NativeLong dwNumberOfBytesToFlush);
    }


    private void streamWrite(WinNT.HANDLE hFile, InputStream is, String absPath) throws IOException {
        byte[] buffer = new byte[65536];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            if (!LocalKernel32.INSTANCE.WriteFile(hFile, buffer, bytesRead, null, null)) {
                throw new IOException("Unable to write " + absPath + ", error code: " + LocalKernel32.INSTANCE.GetLastError());
            }
        }
    }

    @Override
    public void saveBytesTo(Path path, InputStream inputStream) throws IOException {
        String absPath = path.toAbsolutePath().toString();
        Path tempPath = path.resolveSibling(path.getFileName().toString() + ".tmp");
        String absTempPath = tempPath.toAbsolutePath().toString();

        try {
            internalSave(absTempPath, inputStream);
        } catch (IOException e) {
            try { Files.deleteIfExists(tempPath); } catch (Exception ignored) {}
            throw e;
        }

        boolean success = LocalKernel32.INSTANCE.MoveFileEx(
                absTempPath,
                absPath,
                new WinDef.DWORD(0x00000001 | 0x00000008)
        );

        if (!success) {
            int err = LocalKernel32.INSTANCE.GetLastError();
            try { Files.deleteIfExists(tempPath); } catch (Exception ignored) {}
            throw new IOException("Failed to replace file with shadow copy: " + absPath + ", Error: " + err);
        }
    }


    private void internalSave(String absTempPath, InputStream inputStream) throws IOException {
        WinNT.HANDLE hFile = LocalKernel32.INSTANCE.CreateFile(
                absTempPath,
                WinNT.GENERIC_READ | WinNT.GENERIC_WRITE,
                0x00000007,
                null,
                WinNT.CREATE_ALWAYS,
                WinNT.FILE_ATTRIBUTE_NORMAL,
                null
        );

        if (WinBase.INVALID_HANDLE_VALUE.equals(hFile)) {
            throw new IOException("Unable to open temp file handle: " + absTempPath + ", error code: " + LocalKernel32.INSTANCE.GetLastError());
        }

        try {
            streamWrite(hFile, inputStream, absTempPath);
        } finally {
            LocalKernel32.INSTANCE.CloseHandle(hFile);
        }
    }

    @Override
    public ByteBuffer readBytesFrom(Path path) throws IOException {
        String absPath = path.toAbsolutePath().toString();

        // 0x00000007 all share
        WinNT.HANDLE hFile = LocalKernel32.INSTANCE.CreateFile(
                absPath,
                WinNT.GENERIC_READ,
                0x00000007,
                null,
                WinNT.OPEN_EXISTING,
                WinNT.FILE_ATTRIBUTE_NORMAL,
                null
        );

        if (WinBase.INVALID_HANDLE_VALUE.equals(hFile)) {
            throw new IOException("Unable to access " + absPath);
        }

        WinNT.HANDLE hMapping = null;
        try {
            WinNT.LARGE_INTEGER sizeStruct = new WinNT.LARGE_INTEGER();
            if (!LocalKernel32.INSTANCE.GetFileSizeEx(hFile, sizeStruct)) {
                throw new IOException("Unable to get file size of " + absPath + ", error code: " + LocalKernel32.INSTANCE.GetLastError());
            }
            long fileSize = sizeStruct.getValue();

            if (fileSize == 0) return ByteBuffer.allocate(0);
            if (fileSize > Integer.MAX_VALUE) throw new IOException("File " + absPath + " is larger then 2GB");

            hMapping = LocalKernel32.INSTANCE.CreateFileMapping(hFile, null, WinNT.PAGE_READONLY, 0, 0, null);
            if (hMapping == null) throw new IOException("Unable to create file map for " + absPath);

            Pointer pAddress = LocalKernel32.INSTANCE.MapViewOfFile(hMapping, WinNT.FILE_MAP_READ, 0, 0, 0);
            if (pAddress == null) throw new IOException("Unable to map view of " + absPath);

            return pAddress.getByteBuffer(0, fileSize);
        } finally {
            if (hMapping != null) LocalKernel32.INSTANCE.CloseHandle(hMapping);
            LocalKernel32.INSTANCE.CloseHandle(hFile);
        }
    }


    @Override
    public boolean isFileReadable(Path path) {
        if (!Files.exists(path)) return false;

        String absPath = path.toAbsolutePath().toString();
        WinNT.HANDLE hFile = LocalKernel32.INSTANCE.CreateFile(
                absPath,
                WinNT.GENERIC_READ,
                0x00000007, // FILE_SHARE_READ | WRITE | DELETE
                null,
                WinNT.OPEN_EXISTING,
                WinNT.FILE_ATTRIBUTE_NORMAL,
                null
        );

        if (WinBase.INVALID_HANDLE_VALUE.equals(hFile)) {
            return false;
        }

        LocalKernel32.INSTANCE.CloseHandle(hFile);
        return true;
    }

    @Override
    public boolean deleteFile(Path path) {
        if (!Files.exists(path)) return true;

        String absPath = path.toAbsolutePath().toString();

        WinNT.HANDLE hFile = LocalKernel32.INSTANCE.CreateFile(
                absPath,
                WinNT.DELETE, // request delete privilege
                0x00000007,   // allow others to read, write and delete
                null,
                WinNT.OPEN_EXISTING,
                WinNT.FILE_FLAG_DELETE_ON_CLOSE, // delete on handle is closed
                null
        );

        if (!WinBase.INVALID_HANDLE_VALUE.equals(hFile)) {
            LocalKernel32.INSTANCE.CloseHandle(hFile);
            return !Files.exists(path);
        }

        return LocalKernel32.INSTANCE.MoveFileEx(
                absPath,
                null,
                new WinDef.DWORD(WinNT.MOVEFILE_DELAY_UNTIL_REBOOT)
        );
    }
}
