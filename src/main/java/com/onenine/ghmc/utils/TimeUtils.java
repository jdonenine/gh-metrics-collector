package com.onenine.ghmc.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

public class TimeUtils {
    private static final ZoneId UTC_ZONE_ID = ZoneId.of(ZoneOffset.UTC.getId());

    public static ZonedDateTime getCurrentUtcDateTime() {
        return ZonedDateTime.now(UTC_ZONE_ID);
    }

    public static ZonedDateTime getUtcDateTimeFromDate(Date date) {
        if (date == null) {
            throw new IllegalArgumentException("date is required");
        }
        return ZonedDateTime.ofInstant(date.toInstant(), UTC_ZONE_ID);
    }

    public static ZonedDateTime getUtcDateTimeFromDate(Instant date) {
        if (date == null) {
            throw new IllegalArgumentException("date is required");
        }
        return ZonedDateTime.ofInstant(date, UTC_ZONE_ID);
    }
}
