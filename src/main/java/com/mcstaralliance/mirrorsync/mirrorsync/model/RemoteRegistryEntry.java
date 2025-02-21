package com.mcstaralliance.mirrorsync.mirrorsync.model;

import java.util.Objects;

public class RemoteRegistryEntry {

    private final String filename;
    private final String hash;
    private final String savePath;
    private final String downloadUrl;

    public RemoteRegistryEntry(String filename, String hash, String savePath, String downloadUrl) {
        this.filename = filename;
        this.hash = hash;
        this.savePath = savePath;
        this.downloadUrl = downloadUrl;
    }

    public String getFilename() {
        return filename;
    }

    public String getHash() {
        return hash;
    }

    public String getSavePath() {
        return savePath;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RemoteRegistryEntry that)) return false;
        return Objects.equals(filename, that.filename) && Objects.equals(hash, that.hash) && Objects.equals(savePath, that.savePath) && Objects.equals(downloadUrl, that.downloadUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename, hash, savePath, downloadUrl);
    }

    @Override
    public String toString() {
        return "RemoteRegistryEntry{" +
                "filename='" + filename + '\'' +
                ", hash='" + hash + '\'' +
                ", savePath='" + savePath + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                '}';
    }
}
