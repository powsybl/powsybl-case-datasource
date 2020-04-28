/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver.parsers;

import com.powsybl.commons.util.ServiceLoaderCache;

import java.util.List;

/**
 * A utility class to work with case name parsers.
 *
 * @author Slimane Amar <slimane.amar at rte-france.com>
 */
public class FileNameParsersServiceLoader implements FileNameParsersLoader {

    private static final ServiceLoaderCache<FileNameParser> PARSER_LOADER = new ServiceLoaderCache<>(FileNameParser.class);

    @Override
    public List<FileNameParser> loadParsers() {
        return PARSER_LOADER.getServices();
    }
}
