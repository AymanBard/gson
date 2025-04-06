package com.google.gson.internal;

import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A registry for managing and caching TypeAdapters.
 */
public class TypeAdapterRegistry {
    private final ConcurrentMap<TypeToken<?>, TypeAdapter<?>> cache = new ConcurrentHashMap<>();

    /**
     * Retrieves a TypeAdapter for the given type. If the adapter is not already cached,
     * it is created using the provided factory and added to the cache.
     *
     * @param <T>    The type for which the adapter is requested.
     * @param type   The TypeToken representing the type.
     * @param factory The factory to create the adapter if not cached.
     * @return The TypeAdapter for the given type.
     * @throws IllegalArgumentException if the factory cannot create an adapter for the type.
     */
    public <T> TypeAdapter<T> getAdapter(TypeToken<T> type, TypeAdapterFactory factory) {
        @SuppressWarnings("unchecked")
        TypeAdapter<T> cachedAdapter = (TypeAdapter<T>) cache.get(type);
        if (cachedAdapter != null) {
            return cachedAdapter;
        }

        TypeAdapter<T> newAdapter = factory.create(null, type);
        if (newAdapter == null) {
            throw new IllegalArgumentException("Factory cannot create TypeAdapter for " + type);
        }

        @SuppressWarnings("unchecked")
        TypeAdapter<T> existingAdapter = (TypeAdapter<T>) cache.putIfAbsent(type, newAdapter);
        return existingAdapter != null ? existingAdapter : newAdapter;
    }
}