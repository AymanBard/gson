package com.google.gson.internal.bind.util;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * A unified TypeAdapter for handling Date serialization and deserialization.
 */
public class UnifiedDateTypeAdapter extends TypeAdapter<Date> {
    private static final String UTC_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final String SQL_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private final SimpleDateFormat utcFormatter;
    private final SimpleDateFormat sqlFormatter;

    public UnifiedDateTypeAdapter() {
        this.utcFormatter = new SimpleDateFormat(UTC_FORMAT, Locale.US);
        this.utcFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        this.sqlFormatter = new SimpleDateFormat(SQL_FORMAT, Locale.US);
    }

    @Override
    public void write(JsonWriter out, Date value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            synchronized (utcFormatter) {
                out.value(utcFormatter.format(value));
            }
        }
    }

    @Override
    public Date read(JsonReader in) throws IOException {
        String dateStr = in.nextString();
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        synchronized (utcFormatter) {
            try {
                return utcFormatter.parse(dateStr);
            } catch (ParseException e) {
                // Try SQL format if UTC format fails
                synchronized (sqlFormatter) {
                    try {
                        return sqlFormatter.parse(dateStr);
                    } catch (ParseException ex) {
                        throw new JsonParseException("Failed to parse date: " + dateStr, ex);
                    }
                }
            }
        }
    }
}