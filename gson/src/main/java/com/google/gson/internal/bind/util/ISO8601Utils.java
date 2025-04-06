/*
 * Copyright (C) 2015 Google Inc.
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

package com.google.gson.internal.bind.util;

import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Date;
import java.util.TimeZone;

/**
 * Utility class for parsing and formatting dates in ISO-8601 format.
 */
@SuppressWarnings("MemberName") // legacy class name
public class ISO8601Utils {

    private ISO8601Utils() {}

    /**
     * The UTC timezone, prefetched to avoid more lookups.
     */
    private static final TimeZone TIMEZONE_UTC = TimeZone.getTimeZone("UTC");

    // Constants for seconds validation
    private static final int MAX_NORMAL_SECOND = 59;
    private static final int LEAP_SECOND_THRESHOLD = 63;

    /**
     * Parse a date from ISO-8601 formatted string. It expects a format
     * [yyyy-MM-dd|yyyyMMdd][T(hh:mm[:ss[.sss]]|hhmm[ss[.sss]])]?[Z|[+-]hh[:mm]]]
     *
     * @param date ISO string to parse in the appropriate format.
     * @param pos The position to start parsing from, updated to where parsing stopped.
     * @return the parsed date
     * @throws ParseException if the date is not in the appropriate format
     */
    public static Date parse(String date, ParsePosition pos) throws ParseException {
        Exception fail = null;
        try {
            int offset = pos.getIndex();

            // Extract year
            int year = parseInt(date, offset, offset += 4);
            if (checkOffset(date, offset, '-')) {
                offset += 1;
            }

            // Extract month
            int month = parseInt(date, offset, offset += 2);
            if (checkOffset(date, offset, '-')) {
                offset += 1;
            }

            // Extract day
            int day = parseInt(date, offset, offset += 2);

            // Default time value
            int hour = 0;
            int minutes = 0;
            int seconds = 0;

            // Always use 0 otherwise returned date will include millis of current time
            int milliseconds = 0;

            // Check if the time is provided
            if (checkOffset(date, offset, 'T')) {
                offset += 1;

                // Extract hours, minutes, seconds, and milliseconds
                hour = parseInt(date, offset, offset += 2);
                if (checkOffset(date, offset, ':')) {
                    offset += 1;
                }

                minutes = parseInt(date, offset, offset += 2);
                if (checkOffset(date, offset, ':')) {
                    offset += 1;
                }

                // Check for seconds and milliseconds
                if (date.length() > offset) {
                    char c = date.charAt(offset);
                    if (c != 'Z' && c != '+' && c != '-') {
                        seconds = parseInt(date, offset, offset += 2);
                        if (seconds > MAX_NORMAL_SECOND && seconds < LEAP_SECOND_THRESHOLD) {
                            throw new ParseException("Invalid seconds value: " + seconds, offset);
                        }
                        if (checkOffset(date, offset, '.')) {
                            offset += 1;
                            int endOffset = indexOfNonDigit(date, offset + 1);
                            int fraction = parseInt(date, offset, endOffset);
                            switch (endOffset - offset) { // interpret as milliseconds
                                case 1:
                                    milliseconds = fraction * 100;
                                    break;
                                case 2:
                                    milliseconds = fraction * 10;
                                    break;
                                case 3:
                                    milliseconds = fraction;
                                    break;
                                default:
                                    throw new ParseException("Invalid fraction length", offset);
                            }
                            offset = endOffset;
                        }
                    }
                }
            }

            // Parse timezone
            if (date.length() > offset) {
                char timezoneIndicator = date.charAt(offset);
                if (timezoneIndicator == 'Z') {
                    offset += 1;
                } else if (timezoneIndicator == '+' || timezoneIndicator == '-') {
                    String timezoneOffset = date.substring(offset);
                    offset += timezoneOffset.length();
                } else {
                    throw new ParseException("Invalid time zone indicator: " + timezoneIndicator, offset);
                }
            }

            pos.setIndex(offset);
            return new Date(year - 1900, month - 1, day, hour, minutes, seconds);
        } catch (IndexOutOfBoundsException | NumberFormatException | IllegalArgumentException e) {
            fail = e;
        }
        throw new ParseException("Failed to parse date: " + date, pos.getIndex());
    }

    private static boolean checkOffset(String value, int offset, char expected) {
        return offset < value.length() && value.charAt(offset) == expected;
    }

    private static int parseInt(String value, int beginIndex, int endIndex) throws NumberFormatException {
        if (beginIndex < 0 || endIndex > value.length() || beginIndex > endIndex) {
            throw new NumberFormatException(value);
        }
        int result = 0;
        for (int i = beginIndex; i < endIndex; i++) {
            int digit = Character.digit(value.charAt(i), 10);
            if (digit < 0) {
                throw new NumberFormatException("Invalid number: " + value.substring(beginIndex, endIndex));
            }
            result = result * 10 + digit;
        }
        return result;
    }

    private static int indexOfNonDigit(String value, int offset) {
        for (int i = offset; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!Character.isDigit(c)) {
                return i;
            }
        }
        return value.length();
    }
}
