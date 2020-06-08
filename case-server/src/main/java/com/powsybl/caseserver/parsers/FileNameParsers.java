/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver.parsers;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

/**
 * A utility class to work with case name parsers.
 *
 * @author Slimane Amar <slimane.amar at rte-france.com>
 */
public final class FileNameParsers {

    private static final Supplier<FileNameParsersLoader> LOADER = Suppliers.memoize(FileNameParsersServiceLoader::new);

    private FileNameParsers() {
    }

    public static FileNameParser findParser(String fileBaseName) {
        return findParser(fileBaseName, LOADER.get());
    }

    public static FileNameParser findParser(String fileBaseName, FileNameParsersLoader loader) {
        for (FileNameParser parser : loader.loadParsers()) {
            if (parser.exists(fileBaseName)) {
                return parser;
            }
        }
        return null;
    }

}

