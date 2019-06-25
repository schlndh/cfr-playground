package com.ggp.parsers.exceptions;

/**
 * Unchecked exception thrown by type factories (e.g. when argument validation fails).
 */
public class TypeFactoryException extends RuntimeException {
    public TypeFactoryException(Throwable throwable) {
        super(throwable);
    }
}
