package com.powsybl.caseserver.parsers;
/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class to work with case name parsers.
 *
 * @author Slimane Amar <slimane.amar at rte-france.com>
 */
public final class FileNameParsers {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileNameParsers.class);

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

