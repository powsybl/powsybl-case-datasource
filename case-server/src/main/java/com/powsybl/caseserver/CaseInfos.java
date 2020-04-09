/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import java.beans.Transient;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ApiModel("Case infos")
public class CaseInfos {

    private static final String NAME_HEADER_KEY     = "name";
    private static final String FORMAT_HEADER_KEY   = "format";

    private String name;
    private String format;

    @Transient
    public Message<String> getMessage() {
        return MessageBuilder.withPayload("")
                .setHeader(NAME_HEADER_KEY,   name)
                .setHeader(FORMAT_HEADER_KEY, format)
                .build();
    }

}
