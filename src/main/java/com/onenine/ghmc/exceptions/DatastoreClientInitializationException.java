package com.onenine.ghmc.exceptions;

public class DatastoreClientInitializationException extends Throwable {
    private static final String DEFAULT_MESSAGE = "Unable to initialize client for datastore";

    public DatastoreClientInitializationException(String m) {
        super(m);
    }

    public DatastoreClientInitializationException(Throwable t) {
        super(DEFAULT_MESSAGE, t);
    }

    public DatastoreClientInitializationException(String m, Throwable t) {
        super(m, t);
    }
}
