/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver;

import com.powsybl.caseserver.dao.CaseInfosDAO;
import com.powsybl.caseserver.dao.elasticsearch.CaseInfosDAOImpl;
import com.powsybl.caseserver.dao.elasticsearch.ESConfig;
import com.powsybl.caseserver.dto.CaseInfos;
import com.powsybl.caseserver.dto.entsoe.EntsoeCaseInfos;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Slimane Amar <slimane.amar at rte-france.com>
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,  classes = {CaseService.class, CaseInfosDAOImpl.class, ESConfig.class})
public class CaseInfosELRepositoryTests {

    private static final String SN_UCTE_CASE_FILE_NAME      = "20200103_0915_SN5_D80.UCT";
    private static final String ID_UCTE_CASE_FILE_NAME      = "20200424_1330_135_CH2.UCT";
    private static final String D1_UCTE_CASE_FILE_NAME      = "20200110_0430_FO5_FR0.uct";
    private static final String D2_UCTE_CASE_FILE_NAME      = "20200430_1530_2D4_D41.uct";
    private static final String TEST_CGMES_CASE_FILE_NAME   = "20200212_1030_FO3_FR1.zip";
    private static final String CASE_FILE_NAME_INCORRECT    = "20200103_0915_SN5.UCT";
    private static final String TEST_OTHER_CASE_FILE_NAME   = "testCase.xiidm";

    @Autowired
    private CaseService caseService;

    @Autowired
    private CaseInfosDAO caseInfosDAO;

    @Test
    public void testAddDeleteCaseInfos() {
        EntsoeCaseInfos caseInfos1 = (EntsoeCaseInfos) createInfos(SN_UCTE_CASE_FILE_NAME);
        caseInfosDAO.addCaseInfos(caseInfos1);
        Optional<CaseInfos> caseInfosAfter1 = caseInfosDAO.getCaseInfosByUuid(caseInfos1.getUuid().toString());
        assertFalse(caseInfosAfter1.isEmpty());
        assertEquals(caseInfos1, caseInfosAfter1.get());

        EntsoeCaseInfos caseInfos2 = (EntsoeCaseInfos) createInfos(ID_UCTE_CASE_FILE_NAME);
        caseInfosDAO.addCaseInfos(caseInfos2);
        Optional<CaseInfos> caseInfosAfter2 = caseInfosDAO.getCaseInfosByUuid(caseInfos2.getUuid().toString());
        assertFalse(caseInfosAfter2.isEmpty());
        assertEquals(caseInfos2, caseInfosAfter2.get());

        caseInfosDAO.deleteCaseInfosByUuid(caseInfos1.getUuid().toString());
        caseInfosAfter1 = caseInfosDAO.getCaseInfosByUuid(caseInfos1.getUuid().toString());
        assertTrue(caseInfosAfter1.isEmpty());

        caseInfosDAO.deleteCaseInfos(caseInfos2);
        caseInfosAfter2 = caseInfosDAO.getCaseInfosByUuid(caseInfos2.getUuid().toString());
        assertTrue(caseInfosAfter2.isEmpty());
    }

    private CaseInfos createInfos(String fileName) {
        Path casePath = Path.of(this.getClass().getResource("/" + fileName).getPath());
        String fileBaseName = casePath.getFileName().toString();
        String format = caseService.getFormat(casePath);
        return caseService.createInfos(fileBaseName, UUID.randomUUID(), format);
    }

}
