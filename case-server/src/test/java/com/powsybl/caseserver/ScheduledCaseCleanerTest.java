/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver;

import com.powsybl.caseserver.elasticsearch.CaseInfosRepository;
import com.powsybl.caseserver.elasticsearch.DisableElasticsearch;
import com.powsybl.caseserver.repository.CaseMetadataEntity;
import com.powsybl.caseserver.repository.CaseMetadataRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@DisableElasticsearch
public class ScheduledCaseCleanerTest {

    @Autowired
    public CaseMetadataRepository caseMetadataRepository;

    @Autowired
    public ScheduledCaseCleaner scheduledCaseCleaner;

    @MockBean
    CaseInfosRepository caseInfosRepository;

    @MockBean
    public CaseService caseService;

    @Before
    public void setUp() {
        cleanDB();
    }

    private void cleanDB() {
        caseMetadataRepository.deleteAll();
    }

    @Test
    public void test() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime yesterday = now.minusDays(1);
        CaseMetadataEntity shouldNotExpireEntity = new CaseMetadataEntity(UUID.randomUUID(), now.plusHours(1));
        CaseMetadataEntity shouldExpireEntity = new CaseMetadataEntity(UUID.randomUUID(), yesterday.plusHours(1));
        CaseMetadataEntity noExpireDateEntity = new CaseMetadataEntity(UUID.randomUUID(), null);
        caseMetadataRepository.save(shouldExpireEntity);
        caseMetadataRepository.save(shouldNotExpireEntity);
        caseMetadataRepository.save(noExpireDateEntity);
        assertEquals(3, caseMetadataRepository.findAll().size());
        scheduledCaseCleaner.deleteExpiredCases();
        assertEquals(2, caseMetadataRepository.findAll().size());
        assertTrue(caseMetadataRepository.findById(shouldNotExpireEntity.getId()).isPresent());
        assertTrue(caseMetadataRepository.findById(noExpireDateEntity.getId()).isPresent());
        assertTrue(caseMetadataRepository.findById(shouldExpireEntity.getId()).isEmpty());
        verify(caseService, times(1)).deleteCase(shouldExpireEntity.getId());
    }

}
