/*
  Copyright (c) 2019, RTE (http://www.rte-france.com)
  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver.parsers;

import java.util.Optional;

/**
 * A utility class to work with case name parsers.
 *
 * @author Slimane Amar <slimane.amar at rte-france.com>
 */
public interface FileNameParser {

    boolean exists(String fileBaseName);

    default Optional<? extends FileNameInfos> parse(String fileBaseName) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
