/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver;

import java.nio.file.Path;
import java.util.Objects;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
public final class CaseException extends RuntimeException {

    public enum Type {
        FILE_ALREADY_EXISTS,
        FILE_DOESNT_EXIST,
        FILE_NOT_IMPORTABLE,
        STORAGE_DIR_NOT_CREATED,
        ILLEGAL_FILE_NAME
    }

    private final Type type;

    private CaseException(Type type, String msg) {
        super(msg);
        this.type = Objects.requireNonNull(type);
    }

    public Type getType() {
        return type;
    }

    public static CaseException createFileAreadyExists(Path file) {
        Objects.requireNonNull(file);
        return new CaseException(Type.FILE_ALREADY_EXISTS, "A file with the same name already exists: " + file);
    }

    public static CaseException createFileDoesNotExist(Path file) {
        Objects.requireNonNull(file);
        return new CaseException(Type.FILE_DOESNT_EXIST, "The file requested doesn't exist: " + file);
    }

    public static CaseException createFileNotImportable(Path file) {
        Objects.requireNonNull(file);
        return new CaseException(Type.FILE_NOT_IMPORTABLE, "This file cannot be imported: " + file);
    }

    public static CaseException createStorageNotInitialized(Path storageRootDir) {
        Objects.requireNonNull(storageRootDir);
        return new CaseException(Type.STORAGE_DIR_NOT_CREATED, "The storage is not initialized: " + storageRootDir);
    }

    public static CaseException createIllegalCaseName(String caseName) {
        Objects.requireNonNull(caseName);
        return new CaseException(Type.ILLEGAL_FILE_NAME, "This is not an acceptable case name: " + caseName);
    }
}
