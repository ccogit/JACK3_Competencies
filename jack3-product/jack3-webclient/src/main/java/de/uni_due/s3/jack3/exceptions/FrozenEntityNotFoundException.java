package de.uni_due.s3.jack3.exceptions;

public class FrozenEntityNotFoundException extends RuntimeException {

    public FrozenEntityNotFoundException(String message) {
        super(message);
    }

    private static final long serialVersionUID = -1762778356760669252L;

}
