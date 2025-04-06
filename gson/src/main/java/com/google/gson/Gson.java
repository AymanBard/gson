/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gson;

import com.google.gson.internal.TypeAdapterRegistry;
import com.google.gson.internal.bind.JsonAdapterAnnotationTypeAdapterFactory;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.internal.bind.util.UnifiedDateTypeAdapter;
import com.google.gson.reflect.TypeToken;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Gson main class with updated TypeAdapter management.
 */
public final class Gson {
    private final TypeAdapterRegistry typeAdapterRegistry = new TypeAdapterRegistry();
    private final JsonAdapterAnnotationTypeAdapterFactory jsonAdapterFactory;
    private final List<TypeAdapterFactory> factories;

    public Gson() {
        // Initialize factories and jsonAdapterFactory as before
        this.factories = List.of(
            TypeAdapters.JSON_ELEMENT_FACTORY,
            TypeAdapters.STRING_FACTORY,
            TypeAdapters.INTEGER_FACTORY,
            // Add other default factories here
            new JsonAdapterAnnotationTypeAdapterFactory(null),
            // Add the unified date adapter
            TypeAdapters.newFactory(Date.class, new UnifiedDateTypeAdapter())
        );
        this.jsonAdapterFactory = new JsonAdapterAnnotationTypeAdapterFactory(null);
    }

    /**
     * Returns the type adapter for the specified type.
     *
     * @param <T>  the type for which to retrieve the adapter
     * @param type the TypeToken representing the type
     * @return a TypeAdapter for type T
     * @throws IllegalArgumentException if this Gson instance cannot handle the type
     */
    public <T> TypeAdapter<T> getAdapter(TypeToken<T> type) {
        Objects.requireNonNull(type, "type must not be null");

        // Use the TypeAdapterRegistry to manage caching and creation
        return typeAdapterRegistry.getAdapter(type, jsonAdapterFactory);
    }
}
