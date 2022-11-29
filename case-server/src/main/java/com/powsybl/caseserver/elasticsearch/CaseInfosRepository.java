/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver.elasticsearch;

import com.powsybl.caseserver.dto.CaseInfos;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * A class to launch an embedded DB elasticsearch
 *
 * @author Slimane Amar <slimane.amar at rte-france.com>
 */
@Lazy
public interface CaseInfosRepository extends ElasticsearchRepository<CaseInfos, String> {

    Page<CaseInfos> findByUuid(String id, Pageable pageable);

}
