/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver.elasticsearch;

import com.powsybl.caseserver.dto.CaseInfos;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.springframework.lang.NonNull;

/**
 * An interface to define an api for metadatas transfer in the DB elasticsearch
 *
 * @author Slimane Amar <slimane.amar at rte-france.com>
 */
public interface CaseInfosService {

    CaseInfos addCaseInfos(@NonNull final CaseInfos ci);

    List<CaseInfos> getAllCaseInfos();

    Optional<CaseInfos> getCaseInfosByUuid(@NonNull final String uuid);

    List<CaseInfos> searchCaseInfos(@NonNull final String query);

    void deleteCaseInfos(@NonNull final CaseInfos ci);

    void deleteCaseInfosByUuid(@NonNull final String uuid);

    void deleteAllCaseInfos();

    static String getDateSearchTerm(@NonNull final DateTime... dates) {
        return Arrays.stream(dates).map(date -> "\"" + date.toDateTimeISO().toString() + "\"").collect(Collectors.joining(" OR ", "date:", "")).toString();
    }

}
