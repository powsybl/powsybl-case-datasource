package com.powsybl.server.cases;

public class CaseException extends RuntimeException {

    public CaseException() {
    }

    public CaseException(String msg) {
        super(msg);
    }

    public CaseException(Throwable throwable) {
        super(throwable);
    }

    public CaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
