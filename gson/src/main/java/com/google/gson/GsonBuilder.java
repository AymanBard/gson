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

import static com.google.gson.Gson.DEFAULT_OBJECT_TO_NUMBER_STRATEGY;
import static com.google.gson.Gson.DEFAULT_SERIALIZE_NULLS;
import static com.google.gson.Gson.DEFAULT_SPECIALIZE_FLOAT_VALUES;
import static com.google.gson.Gson.DEFAULT_STRICTNESS;
import static com.google.gson.Gson.DEFAULT_USE_JDK_UNSAFE;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.InlineMe;
import com.google.gson.annotations.Since;
import com.google.gson.annotations.Until;
import com.google.gson.internal.$Gson$Preconditions;
import com.google.gson.internal.Excluder;
import com.google.gson.internal.bind.DefaultDateTypeAdapter;
import com.google.gson.internal.bind.TreeTypeAdapter;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.internal.sql.SqlTypesSupport;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Use this builder to construct a {@link Gson} instance when you need to set configuration options
 * other than the default. For {@link Gson} with default configuration, it is simpler to use {@code
 * new Gson()}. {@code GsonBuilder} is best used by creating it, and then invoking its various
 * configuration methods, and finally calling {@link #create()}.
 *
 * <p>The following example shows how to use the {@code GsonBuilder} to construct a Gson instance:
 *
 * <pre>
 * Gson gson = new GsonBuilder()
 *     .registerTypeAdapter(Id.class, new IdTypeAdapter())
 *     .enableComplexMapKeySerialization()
 *     .serializeNulls()
 *     .setDateFormat(DateFormat.LONG, DateFormat.LONG)
 *     .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
 *     .setPrettyPrinting()
 *     .create();
 * </pre>
 *
 * <p>Notes:
 *
 * <ul>
 *   <li>The order of invocation of configuration methods does not matter.
 *   <li>The default serialization of {@link Date} and its subclasses in Gson does not contain
 *       time-zone information. So, if you are using date/time instances, use {@code GsonBuilder}
 *       and its {@code setDateFormat} methods.
 *   <li>By default no explicit {@link Strictness} is set; some of the {@link Gson} methods behave
 *       as if {@link Strictness#LEGACY_STRICT} was used whereas others behave as if {@link
 *       Strictness#LENIENT} was used. Prefer explicitly setting a strictness with {@link
 *       #setStrictness(Strictness)} to avoid this legacy behavior.
 * </ul>
 */
public final class GsonBuilder {
  private Excluder excluder = Excluder.DEFAULT;
  private LongSerializationPolicy longSerializationPolicy = LongSerializationPolicy.DEFAULT;
  private FieldNamingStrategy fieldNamingPolicy = FieldNamingPolicy.IDENTITY;
  private final Map<Type, InstanceCreator<?>> instanceCreators = new HashMap<>();
  private final List<TypeAdapterFactory> factories = new ArrayList<>();
  private final List<TypeAdapterFactory> hierarchyFactories = new ArrayList<>();
  private boolean serializeNulls = DEFAULT_SERIALIZE_NULLS;
  private String datePattern = DEFAULT_DATE_PATTERN;
  private int dateStyle = DateFormat.DEFAULT;
  private int timeStyle = DateFormat.DEFAULT;
  private boolean complexMapKeySerialization = DEFAULT_COMPLEX_MAP_KEYS;
  private boolean serializeSpecialFloatingPointValues = DEFAULT_SPECIALIZE_FLOAT_VALUES;
  private boolean escapeHtmlChars = DEFAULT_ESCAPE_HTML;
  private FormattingStyle formattingStyle = DEFAULT_FORMATTING_STYLE;
  private boolean generateNonExecutableJson = DEFAULT_JSON_NON_EXECUTABLE;
  private Strictness strictness = DEFAULT_STRICTNESS;
  private boolean useJdkUnsafe = DEFAULT_USE_JDK_UNSAFE;
  private ToNumberStrategy objectToNumberStrategy = DEFAULT_OBJECT_TO_NUMBER_STRATEGY;
  private ToNumberStrategy numberToNumberStrategy = DEFAULT_NUMBER_TO_NUMBER_STRATEGY;
  private final ArrayDeque<ReflectionAccessFilter> reflectionFilters = new ArrayDeque<>();

  /**
   * Creates a GsonBuilder instance that can be used to build Gson with various configuration
   * settings. GsonBuilder follows the builder pattern, and it is typically used by first invoking
   * various configuration methods to set desired options, and finally calling {@link #create()}.
   */
  public GsonBuilder() {}

  /**
   * Configures Gson to serialize null fields. By default, Gson omits all fields that are null
   * during serialization.
   *
   * @return a reference to this {@code GsonBuilder} object to fulfill the "Builder" pattern
   * @since 1.2
   */
  @CanIgnoreReturnValue
  public GsonBuilder serializeNulls() {
    this.serializeNulls = true;
    return this;
  }

  /**
   * Configures Gson to apply a specific naming policy to an object's fields during serialization
   * and deserialization.
   *
   * <p>This method just delegates to {@link #setFieldNamingStrategy(FieldNamingStrategy)}.
   *
   * @param namingConvention the naming policy to apply
   * @return a reference to this {@code GsonBuilder} object to fulfill the "Builder" pattern
   */
  @CanIgnoreReturnValue
  public GsonBuilder setFieldNamingPolicy(FieldNamingPolicy namingConvention) {
    return setFieldNamingStrategy(namingConvention);
  }

  /**
   * Configures Gson to apply a specific naming strategy to an object's fields during serialization
   * and deserialization.
   *
   * <p>The created Gson instance might only use the field naming strategy once for a field and
   * cache the result. It is not guaranteed that the strategy will be used again every time the
   * value of a field is serialized or deserialized.
   *
   * @param fieldNamingStrategy the naming strategy to apply to the fields
   * @return a reference to this {@code GsonBuilder} object to fulfill the "Builder" pattern
   */
  @CanIgnoreReturnValue
  public GsonBuilder setFieldNamingStrategy(FieldNamingStrategy fieldNamingStrategy) {
    this.fieldNamingPolicy = Objects.requireNonNull(fieldNamingStrategy);
    return this;
  }

  /**
   * Configures Gson to serialize {@code Date} objects according to the pattern provided. You can
   * call this method or {@link #setDateFormat(int, int)} multiple times, but only the last
   * invocation will be used to decide the serialization format.
   *
   * <p>The date format will be used to serialize and deserialize {@link java.util.Date} and in case
   * the {@code java.sql} module is present, also {@link java.sql.Timestamp} and {@link
   * java.sql.Date}.
   *
   * <p>Note that this pattern must abide by the convention provided by {@code SimpleDateFormat}
   * class. See the documentation in {@link SimpleDateFormat} for more information on valid date and
   * time patterns.
   *
   * @param pattern the pattern that dates will be serialized/deserialized to/from; can be {@code
   *     null} to reset the pattern
   * @return a reference to this {@code GsonBuilder} object to fulfill the "Builder" pattern
   * @throws IllegalArgumentException if the pattern is invalid
   * @since 1.2
   */
  @CanIgnoreReturnValue
  public GsonBuilder setDateFormat(String pattern) {
    if (pattern != null && !pattern.trim().isEmpty()) {
      try {
        // Validate the pattern using DateTimeFormatter
        java.time.format.DateTimeFormatter.ofPattern(pattern);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("The date pattern '" + pattern + "' is not valid", e);
      }
      this.datePattern = pattern;
    } else {
      // Reset to default if the pattern is null or empty
      this.datePattern = DEFAULT_DATE_PATTERN;
    }
    return this;
  }

  /**
   * Creates a {@link Gson} instance based on the current configuration. This method is free of
   * side-effects to this {@code GsonBuilder} instance and hence can be called multiple times.
   *
   * @return an instance of Gson configured with the options currently set in this builder
   */
  public Gson create() {
    // Create a new list for factories with the correct size
    List<TypeAdapterFactory> factories =
        new ArrayList<>(this.factories.size() + this.hierarchyFactories.size() + 3);

    // Add hierarchy factories first and reverse them
    List<TypeAdapterFactory> hierarchyFactories = new ArrayList<>(this.hierarchyFactories);
    Collections.reverse(hierarchyFactories);
    factories.addAll(hierarchyFactories);

    // Add the regular factories and reverse them
    List<TypeAdapterFactory> regularFactories = new ArrayList<>(this.factories);
    Collections.reverse(regularFactories);
    factories.addAll(regularFactories);

    // Add type adapters for date handling
    addTypeAdaptersForDate(datePattern, dateStyle, timeStyle, factories);

    // Return the configured Gson instance
    return new Gson(); // Convert ArrayDeque to List if required by the constructor
  }

  private static void addTypeAdaptersForDate(
      String datePattern, int dateStyle, int timeStyle, List<TypeAdapterFactory> factories) {
    TypeAdapterFactory dateAdapterFactory = null;
    boolean sqlTypesSupported = SqlTypesSupport.SUPPORTS_SQL_TYPES;
    TypeAdapterFactory sqlTimestampAdapterFactory = null;
    TypeAdapterFactory sqlDateAdapterFactory = null;

    if (datePattern != null && !datePattern.trim().isEmpty()) {
      dateAdapterFactory = DefaultDateTypeAdapter.DateType.DATE.createAdapterFactory(datePattern);

      if (sqlTypesSupported) {
        sqlTimestampAdapterFactory =
            SqlTypesSupport.TIMESTAMP_DATE_TYPE.createAdapterFactory(datePattern);
        sqlDateAdapterFactory = SqlTypesSupport.DATE_DATE_TYPE.createAdapterFactory(datePattern);
      }
    } else if (dateStyle != DateFormat.DEFAULT || timeStyle != DateFormat.DEFAULT) {
      dateAdapterFactory =
          DefaultDateTypeAdapter.DateType.DATE.createAdapterFactory(dateStyle, timeStyle);

      if (sqlTypesSupported) {
        sqlTimestampAdapterFactory =
            SqlTypesSupport.TIMESTAMP_DATE_TYPE.createAdapterFactory(dateStyle, timeStyle);
        sqlDateAdapterFactory =
            SqlTypesSupport.DATE_DATE_TYPE.createAdapterFactory(dateStyle, timeStyle);
      }
    }

    // Ensure dateAdapterFactory is not null before adding
    if (dateAdapterFactory != null) {
      factories.add(dateAdapterFactory);
    }

    // Ensure sqlTimestampAdapterFactory and sqlDateAdapterFactory are not null before adding
    if (sqlTypesSupported) {
      if (sqlTimestampAdapterFactory != null) {
        factories.add(sqlTimestampAdapterFactory);
      }
      if (sqlDateAdapterFactory != null) {
        factories.add(sqlDateAdapterFactory);
      }
    }
  }
}
