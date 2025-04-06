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

package com.google.gson.internal;

import com.google.gson.ExclusionStrategy;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class selects which fields and types to omit. It is configurable, supporting version
 * attributes {@link Since} and {@link Until}, modifiers, synthetic fields, anonymous and local
 * classes, inner classes, and fields with the {@link Expose} annotation.
 *
 * <p>This class is a type adapter factory; types that are excluded will be adapted to null. It may
 * delegate to another type adapter if only one direction is excluded.
 */
public final class Excluder implements TypeAdapterFactory, Cloneable {
    public static final Excluder DEFAULT = new Excluder();

    private double version = -1.0;
    private int modifiers = Modifier.TRANSIENT | Modifier.STATIC;
    private boolean serializeInnerClasses = true;
    private boolean requireExpose;
    private List<ExclusionStrategy> serializationStrategies = Collections.emptyList();
    private List<ExclusionStrategy> deserializationStrategies = Collections.emptyList();

    @Override
    protected Excluder clone() {
        try {
            return (Excluder) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public Excluder withVersion(double ignoreVersionsAfter) {
        Excluder result = clone();
        result.version = ignoreVersionsAfter;
        return result;
    }

    public Excluder withModifiers(int... modifiers) {
        Excluder result = clone();
        result.modifiers = 0;
        for (int modifier : modifiers) {
            result.modifiers |= modifier;
        }
        return result;
    }

    public Excluder disableInnerClassSerialization() {
        Excluder result = clone();
        result.serializeInnerClasses = false;
        return result;
    }

    public Excluder excludeFieldsWithoutExposeAnnotation() {
        Excluder result = clone();
        result.requireExpose = true;
        return result;
    }

    public Excluder withExclusionStrategy(
        ExclusionStrategy exclusionStrategy, boolean serialization, boolean deserialization) {
        Excluder result = clone();
        if (serialization) {
            result.serializationStrategies = new ArrayList<>(serializationStrategies);
            result.serializationStrategies.add(exclusionStrategy);
        }
        if (deserialization) {
            result.deserializationStrategies = new ArrayList<>(deserializationStrategies);
            result.deserializationStrategies.add(exclusionStrategy);
        }
        return result;
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<?> rawType = type.getRawType();
        boolean excludeClass = excludeClassChecks(rawType);

        if (!excludeClass && !excludeClassInStrategy(rawType, true) && !excludeClassInStrategy(rawType, false)) {
            return null;
        }

        return new TypeAdapter<T>() {
            private volatile TypeAdapter<T> delegate;

            @Override
            public void write(JsonWriter out, T value) throws IOException {
                if (excludeClass) {
                    out.nullValue();
                } else {
                    delegate().write(out, value);
                }
            }

            @Override
            public T read(JsonReader in) throws IOException {
                if (excludeClass) {
                    in.skipValue();
                    return null;
                } else {
                    return delegate().read(in);
                }
            }

            private TypeAdapter<T> delegate() {
                TypeAdapter<T> d = delegate;
                if (d == null) {
                    synchronized (this) {
                        d = delegate;
                        if (d == null) {
                            delegate = d = gson.getDelegateAdapter(Excluder.this, type);
                        }
                    }
                }
                return d;
            }
        };
    }

    private boolean excludeClassChecks(Class<?> clazz) {
        if (version != -1.0 && !isValidVersion(clazz.getAnnotation(Since.class), clazz.getAnnotation(Until.class))) {
            return true;
        }
        if (!serializeInnerClasses && isInnerClass(clazz)) {
            return true;
        }
        if (isAnonymousOrLocal(clazz)) {
            return true;
        }
        return false;
    }

    private boolean excludeClassInStrategy(Class<?> clazz, boolean serialization) {
        List<ExclusionStrategy> strategies = serialization ? serializationStrategies : deserializationStrategies;
        for (ExclusionStrategy strategy : strategies) {
            if (strategy.shouldSkipClass(clazz)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAnonymousOrLocal(Class<?> clazz) {
        return clazz.isAnonymousClass() || clazz.isLocalClass();
    }

    private boolean isInnerClass(Class<?> clazz) {
        return clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers());
    }

    private boolean isValidVersion(Since since, Until until) {
        if (since != null && since.value() > version) {
            return false;
        }
        if (until != null && until.value() <= version) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Excluder{" +
            "version=" + version +
            ", modifiers=" + modifiers +
            ", serializeInnerClasses=" + serializeInnerClasses +
            ", requireExpose=" + requireExpose +
            ", serializationStrategies=" + serializationStrategies +
            ", deserializationStrategies=" + deserializationStrategies +
            '}';
    }
}
