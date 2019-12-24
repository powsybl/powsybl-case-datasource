/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
public final class CaseConstants {

    CaseConstants() {
    }

    static final String CASE_API_VERSION = "v1";

    static final String FILE_ALREADY_EXISTS = "A file with the same name already exists";
    static final String FILE_DOESNT_EXIST = "The file requested doesn't exist";
    static final String DIRECTORY_DOESNT_EXIST = "This directory doesn't exist";
    static final String FILE_NOT_IMPORTABLE = "This file cannot be imported";
    static final String STORAGE_DIR_NOT_CREATED = "The storage is not initialized";

    static final String USERHOME = "user.home";
    static final String CASE_FOLDER = "/cases/";
}
