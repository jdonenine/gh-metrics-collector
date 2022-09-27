package com.onenine.ghmc.exceptions;

public class DatastoreInitializationException extends Throwable {
    private static final String DEFAULT_MESSAGE = "Unable to initialize datastore";

    public DatastoreInitializationException(String m) {
        super(m);
    }

    public DatastoreInitializationException(Throwable t) {
        super(DEFAULT_MESSAGE, t);
    }

    public DatastoreInitializationException(String m, Throwable t) {
        super(m, t);
    }
}
