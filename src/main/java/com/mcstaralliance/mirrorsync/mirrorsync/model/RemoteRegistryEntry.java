package com.mcstaralliance.mirrorsync.mirrorsync.model;

import java.util.Objects;

public record RemoteRegistryEntry(String filename, String hash, String savePath, String downloadUrl) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RemoteRegistryEntry that)) return false;
        return Objects.equals(filename, that.filename) && Objects.equals(hash, that.hash) && Objects.equals(savePath, that.savePath) && Objects.equals(downloadUrl, that.downloadUrl);
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
