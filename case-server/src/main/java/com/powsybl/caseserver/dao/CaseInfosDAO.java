/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver.dao;

import com.powsybl.caseserver.dto.CaseInfos;
import java.io.IOException;
import org.springframework.lang.NonNull;

/**
 * An interface to define an api for metadatas transfer in the DB elasticsearch
 *
 * @author Slimane Amar <slimane.amar at rte-france.com>
 */
public interface CaseInfosDAO {

    void addCaseInfos(@NonNull CaseInfos ci) throws IOException;
/*
    List<CaseInfos> getAllCaseInfos();
    CaseInfos getCaseInfosById(String uuid);
*/
}
