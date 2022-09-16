package com.onenine.ghmc.exceptions;

public class ServiceException extends Throwable {
    private static final String DEFAULT_MESSAGE = "A service exception has occurred";

    public ServiceException(String m) {
        super(m);
    }

    public ServiceException(Throwable t) {
        super(DEFAULT_MESSAGE, t);
    }

    public ServiceException(String m, Throwable t) {
        super(m, t);
    }
}
