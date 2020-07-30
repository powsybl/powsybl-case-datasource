/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver.dto;

import com.powsybl.caseserver.dto.entsoe.EntsoeCaseInfos;
import com.powsybl.caseserver.parsers.FileNameInfos;
import com.powsybl.caseserver.parsers.entsoe.EntsoeFileName;
import io.swagger.annotations.ApiModel;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@SuperBuilder
@NoArgsConstructor
@Getter
@ApiModel("Case infos")
@Document(indexName = "case-server", type = "metadatas")
@TypeAlias(value = "CaseInfos")
public class CaseInfos {

    public static final String NAME_HEADER_KEY   = "name";
    public static final String UUID_HEADER_KEY   = "uuid";
    public static final String FORMAT_HEADER_KEY = "format";

    @Id
    @NonNull protected UUID     uuid;
    @NonNull protected String   name;
    @NonNull protected String   format;

    public static CaseInfos create(String fileBaseName, UUID caseUuid, String format, FileNameInfos fileNameInfos) {
        switch (fileNameInfos.getType()) {
            case ENTSOE:
                EntsoeFileName entsoeFileName = (EntsoeFileName) fileNameInfos;
                return EntsoeCaseInfos.builder().name(fileBaseName).uuid(caseUuid).format(format)
                        .date(entsoeFileName.getDate()).forecastDistance(entsoeFileName.getForecastDistance())
                        .geographicalCode(entsoeFileName.getGeographicalCode()).version(entsoeFileName.getVersion()).build();
            default:
                return CaseInfos.builder().name(fileBaseName).uuid(caseUuid).format(format).build();
        }
    }

    protected MessageBuilder<String> createMessageBuilder() {
        return MessageBuilder.withPayload("")
                .setHeader(NAME_HEADER_KEY,     getName())
                .setHeader(UUID_HEADER_KEY,     getUuid())
                .setHeader(FORMAT_HEADER_KEY,   getFormat());
    }

    public Message<String> createMessage() {
        return createMessageBuilder().build();
    }

}
