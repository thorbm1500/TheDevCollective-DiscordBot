package dev.prodzeus.jarvis.database;

import java.util.EnumMap;

public abstract class DatabaseEntity<Data extends Enum<Data>> {

    private final EnumMap<Data,Object> index;

    protected DatabaseEntity(final Class<Data> enumClass) {
        index = new EnumMap<>(enumClass);
    }

}