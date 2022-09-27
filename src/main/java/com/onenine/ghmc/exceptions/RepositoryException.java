package com.onenine.ghmc.exceptions;

public class RepositoryException extends Throwable {
    private static final String DEFAULT_MESSAGE = "A repository exception has occurred";

    public RepositoryException(String m) {
        super(m);
    }

    public RepositoryException(Throwable t) {
        super(DEFAULT_MESSAGE, t);
    }

    public RepositoryException(String m, Throwable t) {
        super(m, t);
    }
}
