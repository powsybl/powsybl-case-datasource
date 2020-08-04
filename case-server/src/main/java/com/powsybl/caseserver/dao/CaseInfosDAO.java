/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver.dao;

import com.powsybl.caseserver.dto.CaseInfos;
import java.util.Arrays;
import java.util.Collections;
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
public interface CaseInfosDAO {

    default CaseInfos addCaseInfos(@NonNull final CaseInfos ci) {
        return ci;
    }

    default List<CaseInfos> getAllCaseInfos() {
        return Collections.emptyList();
    }

    default Optional<CaseInfos> getCaseInfosByUuid(@NonNull final String uuid) {
        return Optional.empty();
    }

    default List<CaseInfos> searchCaseInfos(@NonNull final String query) {
        return Collections.emptyList();
    }

    default List<CaseInfos> searchCaseInfosByDate(@NonNull final DateTime date) {
        return Collections.emptyList();
    }

    default void deleteCaseInfos(@NonNull final CaseInfos ci) { }

    default void deleteCaseInfosByUuid(@NonNull final String uuid) { }

    default void deleteAllCaseInfos() { }

    static String getDateSearchTerm(@NonNull final DateTime... dates) {
        return Arrays.stream(dates).map(date -> "\"" + date.toDateTimeISO().toString() + "\"").collect(Collectors.joining(" OR ", "date:", "")).toString();
    }

}
