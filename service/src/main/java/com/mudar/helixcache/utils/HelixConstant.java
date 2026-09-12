package com.mudar.helixcache.utils;

public class HelixConstant {

    public static final String TIMEZONE_UTC = "UTC";
    public static final long DEFAULT_CACHE_VERSION = 1L;
    public static final String DEFAULT_NODE_ID = "node-1";
    public static final String HEADER_NODE_ID = "X-Node-Id";
    public static final String SUCCESS_CACHE_ADD = "Cache entry added successfully";
    public static final String SUCCESS_CACHE_UPDATE = "Cache entry updated successfully";
    public static final String SUCCESS_CACHE_REMOVED = "Cache entry removed successfully";
    public static final String ERROR_KEY_EXPIRED = "Cache has expired and will be removed from the cache";
    public static final String ERROR_CACHE_REQUIRED = "Cache entry is required";
    public static final String ERROR_KEY_REQUIRED = "Cache key must not be blank";
    public static final String ERROR_KEY_NOT_EXIST = "Cache key does not exist";
    public static final String ERROR_VALUE_REQUIRED = "Cache value must not be blank";
    public static final String ERROR_EXPIRY_IN_PAST = "Expiry Time must be in the future";
    public static final String INTERNAL_SERVER_ERROR = "Something went wrong. Please try again later.";

    public static final int NODE_SUSPECT_THRESHOLD = 2;
    public static final int NODE_DOWN_THRESHOLD = 4;

}
