/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver;

import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
public final class CaseException extends RuntimeException {

    public enum Type {
        FILE_NOT_IMPORTABLE,
        FILE_NOT_PARSEABLE,
        STORAGE_DIR_NOT_CREATED,
        ILLEGAL_FILE_NAME,
        DIRECTORY_ALREADY_EXISTS,
        DIRECTORY_EMPTY,
        DIRECTORY_NOT_FOUND
    }

    private final Type type;

    private CaseException(Type type, String msg) {
        super(msg);
        this.type = Objects.requireNonNull(type);
    }

    public Type getType() {
        return type;
    }

    public static CaseException createDirectoryAreadyExists(Path directory) {
        Objects.requireNonNull(directory);
        return new CaseException(Type.DIRECTORY_ALREADY_EXISTS, "A directory with the same name already exists: " + directory);
    }

    public static CaseException createDirectoryEmpty(Path directory) {
        Objects.requireNonNull(directory);
        return new CaseException(Type.DIRECTORY_EMPTY, "The directory is empty: " + directory);
    }

    public static CaseException createDirectoryNotFound(UUID uuid) {
        Objects.requireNonNull(uuid);
        return new CaseException(Type.DIRECTORY_NOT_FOUND, "The directory with the following uuid doesn't exist: " + uuid);
    }

    public static CaseException createFileNotImportable(Path file) {
        Objects.requireNonNull(file);
        return new CaseException(Type.FILE_NOT_IMPORTABLE, "This file cannot be imported: " + file);
    }

    public static CaseException createFileNameNotParseable(Path file) {
        Objects.requireNonNull(file);
        return new CaseException(Type.FILE_NOT_PARSEABLE, "This file name cannot be parsed: " + file);
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
