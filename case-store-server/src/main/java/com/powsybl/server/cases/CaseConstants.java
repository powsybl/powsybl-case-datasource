/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.server.cases;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
public final class CaseConstants {

    CaseConstants() {

    }

    public static final String DONE = "DONE";

    public static final String IOEXCEPTION_MESSAGE = "An error occurred while accessing to the storage";
    public static final String FILE_ALREADY_EXISTS = "A file with the same name already exists";
    public static final String IMPORT_FAIL = "An error occurred while importing the file";
    public static final String GET_FILE_FAIL = "An error occurred while trying to get the file";
    public static final String FILE_DOESNT_EXIST = "The file requested doesn't exist";
    public static final String DIRECTORY_DOESNT_EXIST = "This directory doesn't exist";
    public static final String FILE_NOT_IMPORTABLE = "This file cannot be imported";
    public static final String STORAGE_DIR_NOT_CREATED = "The storage is not initialized";

    public static final String USERHOME = "user.home";
    public static final String CASE_FOLDER = "/cases/";

}
