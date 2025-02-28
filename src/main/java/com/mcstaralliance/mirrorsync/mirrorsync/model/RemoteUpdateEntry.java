package com.mcstaralliance.mirrorsync.mirrorsync.model;

import java.util.Objects;
import java.util.Set;

public record RemoteUpdateEntry(String dirPath, Set<String> children) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RemoteUpdateEntry that)) return false;
        return Objects.equals(dirPath, that.dirPath) && Objects.equals(children, that.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dirPath, children);
    }
}
