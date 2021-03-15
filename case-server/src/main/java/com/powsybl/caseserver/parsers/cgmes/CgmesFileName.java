/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver.parsers.cgmes;

import com.powsybl.caseserver.parsers.FileNameInfos;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import org.joda.time.DateTime;

/**
 * A utility class to work with Cgmes file name parsers.
 *
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@SuperBuilder
@Getter
public class CgmesFileName implements FileNameInfos {

    @NonNull private DateTime date;
    @NonNull private String businessProcess;
    @NonNull private String tso;
    @NonNull private Integer version;

    public Type getType() {
        return Type.CGMES;
    }
}
