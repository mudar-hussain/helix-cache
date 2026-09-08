package com.mudar.helixcache.utils;


import com.mudar.helixcache.exception.HelixValidationException;
import com.mudar.helixcache.model.Cache;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.util.Strings;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@NoArgsConstructor
public class HelixUtils {

    public static Timestamp getCurrentTimestamp() {
        return new Timestamp(
                ZonedDateTime.now(ZoneId.of(HelixConstant.TIMEZONE_UTC)).toInstant().toEpochMilli()
        );
    }

    public static Timestamp convertToTimestamp(LocalDateTime localDateTime) {
        return new Timestamp(
                localDateTime.atZone(ZoneId.of(HelixConstant.TIMEZONE_UTC)).toInstant().toEpochMilli()
        );
    }

    public static boolean isExpired(Timestamp expiresAt) {
        if(expiresAt == null) return false;
        return !isFuture(expiresAt);
    }

    public static boolean isFuture(Timestamp timestamp) {
        if(timestamp == null) return false;
        return getCurrentTimestamp().before(timestamp);
    }

    public static void validateKeyValueExpiresAtForCreate(String key, String value, Timestamp expiresAt) {
        HelixUtils.validateExpiresAtForCreate(expiresAt);
        HelixUtils.validateKeyValue(key, value);
    }

    public static void validateExpiresAtForCreate(Timestamp expiresAt) {
        if(HelixUtils.isExpired(expiresAt)) {
            throw new HelixValidationException(HelixConstant.ERROR_EXPIRY_IN_PAST);
        }
    }

    public static void validateKeyValue(String key, String value) {
        HelixUtils.validateKey(key);
        HelixUtils.validateValue(value);
    }

    public static void validateKeyCache(String key, Cache cache) {
        validateKey(key);
        validateCache(cache);
    }

    public static void validateKey(String key) {
        if(Strings.isBlank(key)) {
            throw new HelixValidationException(HelixConstant.ERROR_KEY_REQUIRED);
        }
    }

    public static void validateValue(String value) {
        if(Strings.isBlank(value)) {
            throw new HelixValidationException(HelixConstant.ERROR_VALUE_REQUIRED);
        }
    }

    public static void validateCache(Cache cache) {
        if(cache == null) {
            throw new NullPointerException(HelixConstant.ERROR_CACHE_REQUIRED);
        }
    }
}
