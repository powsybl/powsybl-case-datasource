/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static com.powsybl.caseserver.CaseConstants.*;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
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
            case STORAGE_DIR_NOT_CREATED :
                return handleExceptionInternal(ex, STORAGE_DIR_NOT_CREATED, new HttpHeaders(), HttpStatus.UNPROCESSABLE_ENTITY, request);
            default:
                return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
        }
    }
}
