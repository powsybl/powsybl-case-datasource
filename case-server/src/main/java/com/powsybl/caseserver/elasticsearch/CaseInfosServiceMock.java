/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver.elasticsearch;

import com.powsybl.caseserver.dto.CaseInfos;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.lang.NonNull;

/**
 * An interface to define an api for metadatas transfer in the DB elasticsearch
 *
 * @author Slimane Amar <slimane.amar at rte-france.com>
 */
public class CaseInfosServiceMock implements CaseInfosService {

    @Override
    public CaseInfos addCaseInfos(@NonNull final CaseInfos ci) {
        return ci;
    }

    @Override
    public List<CaseInfos> getAllCaseInfos() {
        return Collections.emptyList();
    }

    @Override
    public Optional<CaseInfos> getCaseInfosByUuid(@NonNull final String uuid) {
        return Optional.empty();
    }

    @Override
    public List<CaseInfos> searchCaseInfos(@NonNull final String query) {
        return Collections.emptyList();
    }

    @Override
    public void deleteCaseInfos(@NonNull final CaseInfos ci) { }

    @Override
    public void deleteCaseInfosByUuid(@NonNull final String uuid) { }

    @Override
    public void deleteAllCaseInfos() { }
}
