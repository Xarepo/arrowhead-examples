package eu.arrowhead.core.common;

import java.util.Objects;

public class PropertyException extends Exception {

    PropertyException(final String message) {
        super(Objects.requireNonNull(message));
    }

    PropertyException(final String message, final Throwable e) {
        super(Objects.requireNonNull(message), Objects.requireNonNull(e));
    }

}
