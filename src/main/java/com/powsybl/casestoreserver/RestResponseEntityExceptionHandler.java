package com.powsybl.casestoreserver;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static com.powsybl.casestoreserver.CaseConstants.*;

@ControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = { CaseException.class})
    protected ResponseEntity<Object> handleConflict(RuntimeException ex, WebRequest request) {
        String errorMessage = ex.getMessage();
        switch (errorMessage) {
            case FILE_ALREADY_EXISTS :
                return handleExceptionInternal(ex, FILE_ALREADY_EXISTS, new HttpHeaders(), HttpStatus.CONFLICT, request);
            case FILE_NOT_IMPORTABLE :
                return handleExceptionInternal(ex, FILE_NOT_IMPORTABLE, new HttpHeaders(), HttpStatus.UNPROCESSABLE_ENTITY, request);
            case FILE_DOESNT_EXIST :
                return handleExceptionInternal(ex, FILE_DOESNT_EXIST, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
            case DIRECTORY_DOESNT_EXIST :
                return handleExceptionInternal(ex, DIRECTORY_DOESNT_EXIST, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
            default:
                return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
        }
    }
}
