/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gson.typeadapters;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * A TypeAdapter for serializing and deserializing {@link Date} objects in UTC format.
 */
public final class UtcDateTypeAdapter extends TypeAdapter<Date> {
    private static final String UTC_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

    private final SimpleDateFormat utcFormatter;

    public UtcDateTypeAdapter() {
        this.utcFormatter = new SimpleDateFormat(UTC_FORMAT, Locale.US);
        this.utcFormatter.setTimeZone(UTC_TIME_ZONE);
    }

    @Override
    public void write(JsonWriter out, Date date) throws IOException {
        if (date == null) {
            out.nullValue();
        } else {
            out.value(formatDate(date));
        }
    }

    @Override
    public Date read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        String dateStr = in.nextString();
        return parseDate(dateStr);
    }

    /**
     * Formats the given {@link Date} object into a UTC string.
     *
     * @param date the date to format
     * @return the formatted date string
     */
    private String formatDate(Date date) {
        synchronized (utcFormatter) {
            return utcFormatter.format(date);
        }
    }

    /**
     * Parses the given date string in UTC format.
     *
     * @param dateStr the date string to parse
     * @return the parsed {@link Date} object
     * @throws JsonParseException if the date string cannot be parsed
     */
    private Date parseDate(String dateStr) {
        synchronized (utcFormatter) {
            try {
                return utcFormatter.parse(dateStr);
            } catch (ParseException e) {
                throw new JsonParseException("Failed to parse date: " + dateStr, e);
            }
        }
    }
}
