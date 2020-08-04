/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver;

import com.powsybl.caseserver.dao.CaseInfosDAO;
import com.powsybl.caseserver.dto.CaseInfos;
import com.powsybl.caseserver.dto.entsoe.EntsoeCaseInfos;
import java.nio.file.Path;
import java.util.List;
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,  properties = {"spring.data.elasticsearch.enabled=true"})
public class CaseInfosELRepositoryTests {

    private static final String SN_UCTE_CASE_FILE_NAME      = "20200103_0915_SN5_D80.UCT";
    private static final String ID1_UCTE_CASE_FILE_NAME     = "20200103_0915_135_CH2.UCT";
    private static final String ID2_UCTE_CASE_FILE_NAME     = "20200424_1330_135_CH2.UCT";
    private static final String FO1_UCTE_CASE_FILE_NAME     = "20200103_0915_FO5_FR0.UCT";
    private static final String FO2_UCTE_CASE_FILE_NAME     = "20200110_0430_FO5_FR0.uct";
    private static final String D4_UCTE_CASE_FILE_NAME      = "20200430_1530_2D4_D41.uct";
    private static final String TEST_CGMES_CASE_FILE_NAME   = "20200212_1030_FO3_FR1.zip";

    @Autowired
    private CaseService caseService;

    @Autowired
    private CaseInfosDAO caseInfosDAO;

    @Test
    public void testAddDeleteCaseInfos() {
        EntsoeCaseInfos caseInfos1 = (EntsoeCaseInfos) caseInfosDAO.addCaseInfos(createInfos(SN_UCTE_CASE_FILE_NAME));
        Optional<CaseInfos> caseInfosAfter1 = caseInfosDAO.getCaseInfosByUuid(caseInfos1.getUuid().toString());
        assertFalse(caseInfosAfter1.isEmpty());
        assertEquals(caseInfos1, caseInfosAfter1.get());

        EntsoeCaseInfos caseInfos2 = (EntsoeCaseInfos) caseInfosDAO.addCaseInfos(createInfos(ID2_UCTE_CASE_FILE_NAME));
        Optional<CaseInfos> caseInfosAfter2 = caseInfosDAO.getCaseInfosByUuid(caseInfos2.getUuid().toString());
        assertFalse(caseInfosAfter2.isEmpty());
        assertEquals(caseInfos2, caseInfosAfter2.get());

        caseInfosDAO.deleteCaseInfosByUuid(caseInfos1.getUuid().toString());
        caseInfosAfter1 = caseInfosDAO.getCaseInfosByUuid(caseInfos1.getUuid().toString());
        assertTrue(caseInfosAfter1.isEmpty());

        caseInfosDAO.deleteCaseInfos(caseInfos2);
        caseInfosAfter2 = caseInfosDAO.getCaseInfosByUuid(caseInfos2.getUuid().toString());
        assertTrue(caseInfosAfter2.isEmpty());

        caseInfosDAO.addCaseInfos(caseInfos1);
        caseInfosDAO.addCaseInfos(caseInfos2);
        List<CaseInfos> all = caseInfosDAO.getAllCaseInfos();
        assertFalse(all.isEmpty());
        caseInfosDAO.deleteAllCaseInfos();
        all = caseInfosDAO.getAllCaseInfos();
        assertTrue(all.isEmpty());
    }

    @Test
    public void searchCaseInfos() {
        caseInfosDAO.deleteAllCaseInfos();
        List<CaseInfos> all = caseInfosDAO.getAllCaseInfos();
        assertTrue(all.isEmpty());

        EntsoeCaseInfos ucte1 = (EntsoeCaseInfos) caseInfosDAO.addCaseInfos(createInfos(SN_UCTE_CASE_FILE_NAME));
        EntsoeCaseInfos ucte2 = (EntsoeCaseInfos) caseInfosDAO.addCaseInfos(createInfos(ID1_UCTE_CASE_FILE_NAME));
        EntsoeCaseInfos ucte3 = (EntsoeCaseInfos) caseInfosDAO.addCaseInfos(createInfos(ID2_UCTE_CASE_FILE_NAME));
        EntsoeCaseInfos ucte4 = (EntsoeCaseInfos) caseInfosDAO.addCaseInfos(createInfos(FO1_UCTE_CASE_FILE_NAME));
        EntsoeCaseInfos ucte5 = (EntsoeCaseInfos) caseInfosDAO.addCaseInfos(createInfos(FO2_UCTE_CASE_FILE_NAME));
        EntsoeCaseInfos ucte6 = (EntsoeCaseInfos) caseInfosDAO.addCaseInfos(createInfos(D4_UCTE_CASE_FILE_NAME));
        EntsoeCaseInfos cgmes = (EntsoeCaseInfos) caseInfosDAO.addCaseInfos(createInfos(TEST_CGMES_CASE_FILE_NAME));

        all = caseInfosDAO.searchCaseInfos("*");
        assertFalse(all.isEmpty());
        assertTrue(all.contains(ucte1));
        assertTrue(all.contains(ucte2));
        assertTrue(all.contains(ucte3));
        assertTrue(all.contains(ucte4));
        assertTrue(all.contains(ucte5));
        assertTrue(all.contains(ucte6));
        assertTrue(all.contains(cgmes));

        List<CaseInfos> list = caseInfosDAO.searchCaseInfosByDate(ucte1.getDate());
        assertTrue(list.size() == 3 && list.contains(ucte1) && list.contains(ucte2) && list.contains(ucte4));
        list = caseInfosDAO.searchCaseInfosByDate(ucte3.getDate());
        assertTrue(list.size() == 1 && list.contains(ucte3));
        list = caseInfosDAO.searchCaseInfosByDate(ucte5.getDate());
        assertTrue(list.size() == 1 && list.contains(ucte5));
        list = caseInfosDAO.searchCaseInfosByDate(ucte6.getDate());
        assertTrue(list.size() == 1 && list.contains(ucte6));
        list = caseInfosDAO.searchCaseInfosByDate(cgmes.getDate());
        assertTrue(list.size() == 1 && list.contains(cgmes));

        list = caseInfosDAO.searchCaseInfos("geographicalCode:(D8)");
        assertTrue(list.size() == 1 && list.contains(ucte1));
        list = caseInfosDAO.searchCaseInfos("geographicalCode:(CH)");
        assertTrue(list.size() == 2 && list.contains(ucte2) && list.contains(ucte3));
        list = caseInfosDAO.searchCaseInfos("geographicalCode:(FR)");
        assertTrue(list.size() == 3 && list.contains(ucte4) && list.contains(ucte5) && list.contains(cgmes));
        list = caseInfosDAO.searchCaseInfos("geographicalCode:(D4)");
        assertTrue(list.size() == 1 && list.contains(ucte6));

        list = caseInfosDAO.searchCaseInfos(CaseInfosDAO.getDateSearchTerm(ucte1.getDate()) + " AND geographicalCode:(D8)");
        assertTrue(list.size() == 1 && list.contains(ucte1));
        list = caseInfosDAO.searchCaseInfos(CaseInfosDAO.getDateSearchTerm(ucte3.getDate()) + " AND geographicalCode:(CH)");
        assertTrue(list.size() == 1 && list.contains(ucte3));
        list = caseInfosDAO.searchCaseInfos(CaseInfosDAO.getDateSearchTerm(ucte5.getDate()) + " AND geographicalCode:(FR)");
        assertTrue(list.size() == 1 && list.contains(ucte5));
        list = caseInfosDAO.searchCaseInfos(CaseInfosDAO.getDateSearchTerm(ucte6.getDate()) + " AND geographicalCode:(D4)");
        assertTrue(list.size() == 1 && list.contains(ucte6));

        list = caseInfosDAO.searchCaseInfos(CaseInfosDAO.getDateSearchTerm(ucte4.getDate()) + " AND geographicalCode:(FR OR CH OR D8)");
        assertTrue(list.size() == 3 && list.contains(ucte1) && list.contains(ucte2) && list.contains(ucte4));
        list = caseInfosDAO.searchCaseInfos(CaseInfosDAO.getDateSearchTerm(ucte4.getDate()) + " AND geographicalCode:(FR OR CH)");
        assertTrue(list.size() == 2 && list.contains(ucte2) && list.contains(ucte4));
        list = caseInfosDAO.searchCaseInfos(CaseInfosDAO.getDateSearchTerm(ucte4.getDate()) + " AND geographicalCode:(FR OR D8)");
        assertTrue(list.size() == 2 && list.contains(ucte1) && list.contains(ucte4));
        list = caseInfosDAO.searchCaseInfos(CaseInfosDAO.getDateSearchTerm(ucte4.getDate()) + " AND geographicalCode:(CH OR D8)");
        assertTrue(list.size() == 2 && list.contains(ucte1) && list.contains(ucte2));

        list = caseInfosDAO.searchCaseInfos("geographicalCode:(D4 OR D8)");
        assertTrue(list.size() == 2 && list.contains(ucte1) && list.contains(ucte6));

        list = caseInfosDAO.searchCaseInfos(CaseInfosDAO.getDateSearchTerm(ucte6.getDate()) + " OR " + CaseInfosDAO.getDateSearchTerm(cgmes.getDate()));
        assertTrue(list.size() == 2 && list.contains(ucte6) && list.contains(cgmes));

        list = caseInfosDAO.searchCaseInfos(CaseInfosDAO.getDateSearchTerm(ucte6.getDate(), cgmes.getDate()));
        assertTrue(list.size() == 2 && list.contains(ucte6) && list.contains(cgmes));

        list = caseInfosDAO.searchCaseInfos(CaseInfosDAO.getDateSearchTerm(ucte1.getDate()) + " AND geographicalCode:D8 AND forecastDistance:0");
        assertTrue(list.size() == 1 && list.contains(ucte1));
    }

    private CaseInfos createInfos(String fileName) {
        Path casePath = Path.of(this.getClass().getResource("/" + fileName).getPath());
        String fileBaseName = casePath.getFileName().toString();
        String format = caseService.getFormat(casePath);
        return caseService.createInfos(fileBaseName, UUID.randomUUID(), format);
    }
}
