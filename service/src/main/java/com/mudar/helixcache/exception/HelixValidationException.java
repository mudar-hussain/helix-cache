package com.mudar.helixcache.exception;

public class HelixValidationException extends RuntimeException{

    public HelixValidationException(String message) {
        super(message);
    }

    public HelixValidationException(String message, Throwable th) {
        super(message, th);
    }
}
