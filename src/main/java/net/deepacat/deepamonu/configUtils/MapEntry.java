package net.deepacat.deepamonu.configUtils;

import java.util.Objects;

public class MapEntry {
    public String key;
    public int color;

    public MapEntry(String key, int color) {
        this.key = key;
        this.color = color;
    }

    public MapEntry(MapEntry other) {
        this.key = other.key;
        this.color = other.color;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof MapEntry entry)) return false;
        return color == entry.color && Objects.equals(key, entry.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, color);
    }
}