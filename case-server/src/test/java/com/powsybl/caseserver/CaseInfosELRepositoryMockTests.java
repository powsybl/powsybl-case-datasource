/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver;

import com.powsybl.caseserver.dto.CaseInfos;
import com.powsybl.caseserver.dto.entsoe.EntsoeCaseInfos;
import com.powsybl.caseserver.elasticsearch.CaseInfosService;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertTrue;

/**
 * @author Slimane Amar <slimane.amar at rte-france.com>
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {"spring.data.elasticsearch.enabled=false"})
public class CaseInfosELRepositoryMockTests {

    private static final String SN_UCTE_CASE_FILE_NAME  = "20200103_0915_SN5_D80.UCT";
    private static final String ID2_UCTE_CASE_FILE_NAME = "20200424_1330_135_CH2.UCT";

    @Autowired
    private CaseService caseService;

    @Autowired
    private CaseInfosService caseInfosService;

    @Test
    public void testServiceMock() {
        EntsoeCaseInfos caseInfos1 = (EntsoeCaseInfos) caseInfosService.addCaseInfos(createInfos(SN_UCTE_CASE_FILE_NAME));
        Optional<CaseInfos> caseInfosAfter1 = caseInfosService.getCaseInfosByUuid(caseInfos1.getUuid().toString());
        assertTrue(caseInfosAfter1.isEmpty());

        EntsoeCaseInfos caseInfos2 = (EntsoeCaseInfos) caseInfosService.addCaseInfos(createInfos(ID2_UCTE_CASE_FILE_NAME));
        Optional<CaseInfos> caseInfosAfter2 = caseInfosService.getCaseInfosByUuid(caseInfos2.getUuid().toString());
        assertTrue(caseInfosAfter2.isEmpty());

        caseInfosService.deleteCaseInfosByUuid(caseInfos1.getUuid().toString());
        caseInfosAfter1 = caseInfosService.getCaseInfosByUuid(caseInfos1.getUuid().toString());
        assertTrue(caseInfosAfter1.isEmpty());

        caseInfosService.deleteCaseInfos(caseInfos2);
        caseInfosAfter2 = caseInfosService.getCaseInfosByUuid(caseInfos2.getUuid().toString());
        assertTrue(caseInfosAfter2.isEmpty());

        caseInfosService.addCaseInfos(caseInfos1);
        caseInfosService.addCaseInfos(caseInfos2);

        List<CaseInfos> all = caseInfosService.searchCaseInfos("*");
        assertTrue(all.isEmpty());

        all = caseInfosService.getAllCaseInfos();
        assertTrue(all.isEmpty());

        caseInfosService.deleteAllCaseInfos();
        all = caseInfosService.getAllCaseInfos();
        assertTrue(all.isEmpty());
    }

    private CaseInfos createInfos(String fileName) {
        Path casePath = Path.of(this.getClass().getResource("/" + fileName).getPath());
        String fileBaseName = casePath.getFileName().toString();
        String format = caseService.getFormat(casePath);
        return caseService.createInfos(fileBaseName, UUID.randomUUID(), format);
    }
}
