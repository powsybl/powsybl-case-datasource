/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver.dto.entsoe;

import com.powsybl.caseserver.dto.CaseInfos;
import com.powsybl.entsoe.util.EntsoeGeographicalCode;
import com.powsybl.iidm.network.Country;
import io.swagger.annotations.ApiModel;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import org.joda.time.DateTime;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.messaging.Message;

/**
 * A class to store metatada for Entsoe file name parser
 *
 * @author Slimane Amar <slimane.amar at rte-france.com>
 */
@SuperBuilder
@NoArgsConstructor
@Getter
@ApiModel("Case infos")
@Document(indexName = "case-server", type = "metadatas")
@TypeAlias(value = "EntsoeCaseInfos")
public class EntsoeCaseInfos extends CaseInfos {

    public static final String DATE_HEADER_KEY         = "date";
    public static final String FO_DISTANCE_HEADER_KEY  = "forecastDistance";
    public static final String GEO_CODE_HEADER_KEY     = "geographicalCode";
    public static final String COUNTRY_HEADER_KEY      = "country";
    public static final String VERSION_HEADER_KEY      = "version";

    @NonNull private DateTime date;
    @NonNull private Integer forecastDistance;
    @NonNull private EntsoeGeographicalCode geographicalCode;
    @NonNull private Integer version;

    public Country getCountry() {
        return getGeographicalCode().getCountry();
    }

    @Override
    public Message<String> createMessage() {
        return createMessageBuilder()
            .setHeader(DATE_HEADER_KEY,         getDate())
            .setHeader(FO_DISTANCE_HEADER_KEY,  getForecastDistance())
            .setHeader(GEO_CODE_HEADER_KEY,     getGeographicalCode())
            .setHeader(COUNTRY_HEADER_KEY,      getCountry())
            .setHeader(VERSION_HEADER_KEY,      getVersion())
            .build();
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }

        EntsoeCaseInfos other = (EntsoeCaseInfos) obj;
        return this.date.isEqual(other.date) &&
                Objects.equals(this.forecastDistance, other.forecastDistance) &&
                Objects.equals(this.geographicalCode, other.geographicalCode) &&
                Objects.equals(this.version, other.version);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(date, forecastDistance, geographicalCode, version);
    }
}
