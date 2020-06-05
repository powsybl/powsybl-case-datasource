/*
  Copyright (c) 2019, RTE (http://www.rte-france.com)
  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver.parsers.entsoe;

import com.google.auto.service.AutoService;
import com.powsybl.caseserver.parsers.FileNameParser;
import com.powsybl.entsoe.util.EntsoeGeographicalCode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility class to work with Entsoe file name parsers.
 *
 * @author Slimane Amar <slimane.amar at rte-france.com>
 */
@AutoService(FileNameParser.class)
public class EntsoeFileNameParser implements FileNameParser {

    public static final Pattern FILE_NAME_REGEX = Pattern.compile("(\\d{8}[_]\\d{4})[_](\\w{3})[_](\\w{3}).*");
    public static final String DATE_FORMAT = "yyyyMMdd_HHmm";

    public static DateTime parseDateTime(String dateStr) {
        DateTimeFormatter df = DateTimeFormat.forPattern(DATE_FORMAT);
        return df.withZone(DateTimeZone.forID("Europe/Brussels")).parseDateTime(dateStr);
    }

    @Override
    public boolean exists(String fileBaseName) {
        return FILE_NAME_REGEX.matcher(fileBaseName).matches();
    }

    @Override
    public EntsoeFileName parse(String fileBaseName) {
        Matcher m = FILE_NAME_REGEX.matcher(fileBaseName);
        if (!m.matches()) {
            return null;
        }
        DateTime date = parseDateTime(m.group(1));
        String timeScope = m.group(2).substring(0, 2);
        int forecastDistance;
        EntsoeGeographicalCode geographicalCode;
        int version;
        switch (timeScope) {
            case "SN":
                forecastDistance = 0;
                break;
            case "FO":
                forecastDistance = 60 * (6 + date.getHourOfDay()) + date.getMinuteOfHour();                 // DACF generated at 18:00 one day ahead
                break;
            case "2D":
                forecastDistance = (60 * 24) + (60 * (6 + date.getHourOfDay())) + date.getMinuteOfHour();   // D2CF generated at 18:00 two day ahead
                break;
            default:
                try { // ID ?
                    int hoursID = Integer.parseInt(timeScope);
                    forecastDistance = 60 * hoursID;
                } catch (IllegalArgumentException e) {
                    forecastDistance = 0;
                }
                break;
        }

        geographicalCode = EntsoeGeographicalCode.valueOf(m.group(3).substring(0, 2));
        version = Integer.parseInt(m.group(3).substring(2, 3));

        return EntsoeFileName.builder().date(date).forecastDistance(forecastDistance).geographicalCode(geographicalCode).version(version).build();
    }
}
