/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver.dao.elasticsearch;

import com.powsybl.caseserver.dao.CaseInfosDAO;
import com.powsybl.caseserver.dto.CaseInfos;
import java.io.IOException;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.EntityMapper;
import org.springframework.stereotype.Repository;

import static org.elasticsearch.action.support.WriteRequest.RefreshPolicy.IMMEDIATE;

/**
 * A class to implement metadatas transfer in the DB elasticsearch
 *
 * @author Slimane Amar <slimane.amar at rte-france.com>
 */
@Repository
public class CaseInfosDAOImpl implements CaseInfosDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseInfosDAOImpl.class);

    @Value("${spring.data.elasticsearch.index}")
    private String index;

    @Value("${spring.data.elasticsearch.type}")
    private String mappingType;

    @Autowired
    private RestHighLevelClient elasticsearchClient;

    @Autowired
    private EntityMapper entityMapper;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Override
    public void addCaseInfos(CaseInfos ci) throws IOException  {
        IndexRequest request = new IndexRequest(index, mappingType, ci.getUuid().toString())
                .source(entityMapper.mapObject(ci), XContentType.JSON)
                .setRefreshPolicy(IMMEDIATE);

        IndexResponse res = elasticsearchClient.index(request, RequestOptions.DEFAULT);

        LOGGER.debug(res.toString());
    }
}
