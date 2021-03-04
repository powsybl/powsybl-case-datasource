/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver.parsers.cgmes;

import com.google.auto.service.AutoService;
import com.powsybl.caseserver.parsers.FileNameParser;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility class to work with Entsoe file name parsers.
 *
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@AutoService(FileNameParser.class)
public class CgmesFileNameParser implements FileNameParser {

    public static final Pattern FILE_NAME_REGEX = Pattern.compile("(\\d{8}[T]\\d{4}[Z])[_](\\w{2})[_](.+)[_](\\d{3}).*");
    public static final String DATE_FORMAT = "yyyyMMdd'T'HHmm'Z'";

    public static DateTime parseDateTime(String dateStr) {
        DateTimeFormatter df = DateTimeFormat.forPattern(DATE_FORMAT);
        return df.withZoneUTC().parseDateTime(dateStr);
    }

    @Override
    public boolean exists(String fileBaseName) {
        return FILE_NAME_REGEX.matcher(fileBaseName).matches();
    }

    @Override
    public Optional<CgmesFileName> parse(String fileBaseName) {
        Matcher m = FILE_NAME_REGEX.matcher(fileBaseName);
        if (!m.matches()) {
            return Optional.empty();
        }
        DateTime date = parseDateTime(m.group(1));
        String businessProcess = m.group(2).substring(0, 2);
        String sourcingActor = m.group(3);
        int version = Integer.parseInt(m.group(4).substring(0, 3));

        SourcingActorTsoCode tso = SourcingActorTsoCode.tsoFromSourcingActor(sourcingActor);

        return Optional.of(CgmesFileName.builder().date(date).buisinessProcess(businessProcess).tso(tso).version(version).build());
    }
}
