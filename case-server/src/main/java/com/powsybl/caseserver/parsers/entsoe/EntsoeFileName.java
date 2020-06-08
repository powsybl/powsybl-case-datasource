/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver.parsers.entsoe;

import com.powsybl.caseserver.parsers.FileNameInfos;
import com.powsybl.entsoe.util.EntsoeGeographicalCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import org.joda.time.DateTime;

/**
 * A utility class to work with Entsoe file name parsers.
 *
 * @author Slimane Amar <slimane.amar at rte-france.com>
 */
@SuperBuilder
@Getter
public class EntsoeFileName implements FileNameInfos {

    @NonNull private DateTime date;
    @NonNull private Integer forecastDistance;
    @NonNull private EntsoeGeographicalCode geographicalCode;
    @NonNull private Integer version;

    public Type getType() {
        return Type.ENTSOE;
    }
}
