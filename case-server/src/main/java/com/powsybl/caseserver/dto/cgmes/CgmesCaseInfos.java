/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver.dto.cgmes;

import com.powsybl.caseserver.dto.CaseInfos;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import org.joda.time.DateTime;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.messaging.Message;

import java.util.Objects;

/**
 * A class to store metatada for Cgmes file name parser
 *
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@SuperBuilder
@NoArgsConstructor
@Getter
@Schema(description = "Case infos")
@Document(indexName = "case-server")
@TypeAlias(value = "CgmesCaseInfos")
public class CgmesCaseInfos extends CaseInfos {

    public static final String DATE_HEADER_KEY = "date";
    public static final String BUSINESS_PROCESS_HEADER_KEY = "businessProcess";
    public static final String TSO_HEADER_KEY = "tso";
    public static final String VERSION_HEADER_KEY = "version";

    @NonNull private DateTime date;
    @NonNull private String businessProcess;
    @NonNull private String tso;
    @NonNull private Integer version;

    @Override
    public Message<String> createMessage() {
        return createMessageBuilder()
            .setHeader(DATE_HEADER_KEY, getDate())
            .setHeader(BUSINESS_PROCESS_HEADER_KEY, getBusinessProcess())
            .setHeader(TSO_HEADER_KEY, getTso())
            .setHeader(VERSION_HEADER_KEY, getVersion())
            .build();
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }

        CgmesCaseInfos other = (CgmesCaseInfos) obj;
        return this.date.isEqual(other.date) &&
                Objects.equals(this.businessProcess, other.businessProcess) &&
                Objects.equals(this.tso, other.tso) &&
                Objects.equals(this.version, other.version);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(date.getMillis(), businessProcess, tso, version);
    }
}
