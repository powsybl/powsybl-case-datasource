package com.powsybl.caseserver.dao.elasticsearch;

import com.powsybl.caseserver.dto.CaseInfos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface CaseInfosRepository extends  ElasticsearchRepository<CaseInfos, String> {

    Page<CaseInfos> findByUuid(String id, Pageable pageable);

}
