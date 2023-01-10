/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver.elasticsearch;

import com.google.common.collect.Lists;
import com.powsybl.caseserver.dto.CaseInfos;
import org.elasticsearch.index.query.QueryBuilders;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A class to implement metadatas transfer in the DB elasticsearch
 *
 * @author Slimane Amar <slimane.amar at rte-france.com>
 */
@ComponentScan(basePackageClasses = {CaseInfosRepository.class})
@Service
public class CaseInfosService {

    @Autowired
    @Lazy
    private CaseInfosRepository caseInfosRepository;

    @Autowired
    private ElasticsearchOperations operations;

    public CaseInfos addCaseInfos(@NonNull final CaseInfos ci) {
        caseInfosRepository.save(ci);
        return ci;
    }

    public Optional<CaseInfos> getCaseInfosByUuid(@NonNull final String uuid) {
        Page<CaseInfos> res = caseInfosRepository.findByUuid(uuid,  PageRequest.of(0, 1));
        return res.get().findFirst();
    }

    public List<CaseInfos> getAllCaseInfos() {
        return Lists.newArrayList(caseInfosRepository.findAll());
    }

    public List<CaseInfos> searchCaseInfos(@NonNull final String query) {
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(QueryBuilders.queryStringQuery(query)).build();
        return Lists.newArrayList(operations.search(searchQuery, CaseInfos.class)
                                            .map(searchHit -> searchHit.getContent()));
    }

    public void deleteCaseInfos(@NonNull final CaseInfos ci) {
        caseInfosRepository.delete(ci);
    }

    public void deleteCaseInfosByUuid(@NonNull  String uuid) {
        caseInfosRepository.deleteById(uuid);
    }

    public void deleteAllCaseInfos() {
        caseInfosRepository.deleteAll(getAllCaseInfos());
    }

    public void recreateAllCaseInfos(List<CaseInfos> caseInfos) {
        caseInfosRepository.deleteAll();
        caseInfosRepository.saveAll(caseInfos);
    }

    public static String getDateSearchTerm(@NonNull final DateTime... dates) {
        return Arrays.stream(dates).map(date -> "\"" + date.toDateTimeISO().toString() + "\"").collect(Collectors.joining(" OR ", "date:", "")).toString();
    }
}
