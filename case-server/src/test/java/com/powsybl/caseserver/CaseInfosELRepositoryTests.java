/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver;

import com.google.common.testing.EqualsTester;
import com.powsybl.caseserver.dto.CaseInfos;
import com.powsybl.caseserver.dto.cgmes.CgmesCaseInfos;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Slimane Amar <slimane.amar at rte-france.com>
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {"spring.data.elasticsearch.enabled=true"})
public class CaseInfosELRepositoryTests {

    private static final String SN_UCTE_CASE_FILE_NAME      = "20200103_0915_SN5_D80.UCT";
    private static final String ID1_UCTE_CASE_FILE_NAME     = "20200103_0915_135_CH2.UCT";
    private static final String ID2_UCTE_CASE_FILE_NAME     = "20200424_1330_135_CH2.UCT";
    private static final String FO1_UCTE_CASE_FILE_NAME     = "20200103_0915_FO5_FR0.UCT";
    private static final String FO2_UCTE_CASE_FILE_NAME     = "20200110_0430_FO5_FR0.uct";
    private static final String D4_UCTE_CASE_FILE_NAME      = "20200430_1530_2D4_D41.uct";
    private static final String TEST_CGMES_CASE_FILE_NAME   = "20200424T1330Z_2D_RTEFRANCE_001.zip";

    @Autowired
    private CaseService caseService;

    @Autowired
    private CaseInfosService caseInfosService;

    @Test
    public void testAddDeleteCaseInfos() {
        EntsoeCaseInfos caseInfos1 = (EntsoeCaseInfos) caseInfosService.addCaseInfos(createInfos(SN_UCTE_CASE_FILE_NAME));
        Optional<CaseInfos> caseInfosAfter1 = caseInfosService.getCaseInfosByUuid(caseInfos1.getUuid().toString());
        assertFalse(caseInfosAfter1.isEmpty());
        assertEquals(caseInfos1, caseInfosAfter1.get());
        testEquals(caseInfos1, caseInfosAfter1.get());

        EntsoeCaseInfos caseInfos2 = (EntsoeCaseInfos) caseInfosService.addCaseInfos(createInfos(ID2_UCTE_CASE_FILE_NAME));
        Optional<CaseInfos> caseInfosAfter2 = caseInfosService.getCaseInfosByUuid(caseInfos2.getUuid().toString());
        assertFalse(caseInfosAfter2.isEmpty());
        assertEquals(caseInfos2, caseInfosAfter2.get());
        testEquals(caseInfos2, caseInfosAfter2.get());

        caseInfosService.deleteCaseInfosByUuid(caseInfos1.getUuid().toString());
        caseInfosAfter1 = caseInfosService.getCaseInfosByUuid(caseInfos1.getUuid().toString());
        assertTrue(caseInfosAfter1.isEmpty());

        caseInfosService.deleteCaseInfos(caseInfos2);
        caseInfosAfter2 = caseInfosService.getCaseInfosByUuid(caseInfos2.getUuid().toString());
        assertTrue(caseInfosAfter2.isEmpty());

        caseInfosService.addCaseInfos(caseInfos1);
        caseInfosService.addCaseInfos(caseInfos2);
        List<CaseInfos> all = caseInfosService.getAllCaseInfos();
        assertFalse(all.isEmpty());
        caseInfosService.deleteAllCaseInfos();
        all = caseInfosService.getAllCaseInfos();
        assertTrue(all.isEmpty());
    }

    @Test
    public void searchCaseInfos() {
        caseInfosService.deleteAllCaseInfos();
        List<CaseInfos> all = caseInfosService.getAllCaseInfos();
        assertTrue(all.isEmpty());

        EntsoeCaseInfos ucte1 = (EntsoeCaseInfos) caseInfosService.addCaseInfos(createInfos(SN_UCTE_CASE_FILE_NAME));
        EntsoeCaseInfos ucte2 = (EntsoeCaseInfos) caseInfosService.addCaseInfos(createInfos(ID1_UCTE_CASE_FILE_NAME));
        EntsoeCaseInfos ucte3 = (EntsoeCaseInfos) caseInfosService.addCaseInfos(createInfos(ID2_UCTE_CASE_FILE_NAME));
        EntsoeCaseInfos ucte4 = (EntsoeCaseInfos) caseInfosService.addCaseInfos(createInfos(FO1_UCTE_CASE_FILE_NAME));
        EntsoeCaseInfos ucte5 = (EntsoeCaseInfos) caseInfosService.addCaseInfos(createInfos(FO2_UCTE_CASE_FILE_NAME));
        EntsoeCaseInfos ucte6 = (EntsoeCaseInfos) caseInfosService.addCaseInfos(createInfos(D4_UCTE_CASE_FILE_NAME));
        CgmesCaseInfos cgmes = (CgmesCaseInfos) caseInfosService.addCaseInfos(createInfos(TEST_CGMES_CASE_FILE_NAME));

        all = caseInfosService.searchCaseInfos("*");
        assertFalse(all.isEmpty());
        assertTrue(all.contains(ucte1));
        assertTrue(all.contains(ucte2));
        assertTrue(all.contains(ucte3));
        assertTrue(all.contains(ucte4));
        assertTrue(all.contains(ucte5));
        assertTrue(all.contains(ucte6));
        assertTrue(all.contains(cgmes));

        List<CaseInfos> list = caseInfosService.searchCaseInfos(CaseInfosService.getDateSearchTerm(ucte1.getDate()));
        assertTrue(list.size() == 3 && list.contains(ucte1) && list.contains(ucte2) && list.contains(ucte4));
        list = caseInfosService.searchCaseInfos(CaseInfosService.getDateSearchTerm(ucte3.getDate()));
        assertTrue(list.size() == 1 && list.contains(ucte3));
        list = caseInfosService.searchCaseInfos(CaseInfosService.getDateSearchTerm(ucte5.getDate()));
        assertTrue(list.size() == 1 && list.contains(ucte5));
        list = caseInfosService.searchCaseInfos(CaseInfosService.getDateSearchTerm(ucte6.getDate()));
        assertTrue(list.size() == 1 && list.contains(ucte6));
        list = caseInfosService.searchCaseInfos(CaseInfosService.getDateSearchTerm(cgmes.getDate()));
        assertTrue(list.size() == 1 && list.contains(cgmes));

        list = caseInfosService.searchCaseInfos("geographicalCode:(D8)");
        assertTrue(list.size() == 1 && list.contains(ucte1));
        list = caseInfosService.searchCaseInfos("geographicalCode:(CH)");
        assertTrue(list.size() == 2 && list.contains(ucte2) && list.contains(ucte3));
        list = caseInfosService.searchCaseInfos("geographicalCode:(FR)");
        assertTrue(list.size() == 2 && list.contains(ucte4) && list.contains(ucte5));
        list = caseInfosService.searchCaseInfos("geographicalCode:(D4)");
        assertTrue(list.size() == 1 && list.contains(ucte6));
        list = caseInfosService.searchCaseInfos("tso:(FR)");
        assertTrue(list.size() == 1 && list.contains(cgmes));

        list = caseInfosService.searchCaseInfos(CaseInfosService.getDateSearchTerm(ucte1.getDate()) + " AND geographicalCode:(D8)");
        assertTrue(list.size() == 1 && list.contains(ucte1));
        list = caseInfosService.searchCaseInfos(CaseInfosService.getDateSearchTerm(ucte3.getDate()) + " AND geographicalCode:(CH)");
        assertTrue(list.size() == 1 && list.contains(ucte3));
        list = caseInfosService.searchCaseInfos(CaseInfosService.getDateSearchTerm(ucte5.getDate()) + " AND geographicalCode:(FR)");
        assertTrue(list.size() == 1 && list.contains(ucte5));
        list = caseInfosService.searchCaseInfos(CaseInfosService.getDateSearchTerm(ucte6.getDate()) + " AND geographicalCode:(D4)");
        assertTrue(list.size() == 1 && list.contains(ucte6));
        list = caseInfosService.searchCaseInfos(CaseInfosService.getDateSearchTerm(cgmes.getDate()) + " AND tso:(FR) AND businessProcess:(2D)");
        assertTrue(list.size() == 1 && list.contains(cgmes));

        list = caseInfosService.searchCaseInfos(CaseInfosService.getDateSearchTerm(ucte4.getDate()) + " AND geographicalCode:(FR OR CH OR D8)");
        assertTrue(list.size() == 3 && list.contains(ucte1) && list.contains(ucte2) && list.contains(ucte4));
        list = caseInfosService.searchCaseInfos(CaseInfosService.getDateSearchTerm(ucte4.getDate()) + " AND geographicalCode:(FR OR CH)");
        assertTrue(list.size() == 2 && list.contains(ucte2) && list.contains(ucte4));
        list = caseInfosService.searchCaseInfos(CaseInfosService.getDateSearchTerm(ucte4.getDate()) + " AND geographicalCode:(FR OR D8)");
        assertTrue(list.size() == 2 && list.contains(ucte1) && list.contains(ucte4));
        list = caseInfosService.searchCaseInfos(CaseInfosService.getDateSearchTerm(ucte4.getDate()) + " AND geographicalCode:(CH OR D8)");
        assertTrue(list.size() == 2 && list.contains(ucte1) && list.contains(ucte2));
        list = caseInfosService.searchCaseInfos(CaseInfosService.getDateSearchTerm(cgmes.getDate()) + " AND tso:(ES OR FR OR PT)");
        assertTrue(list.size() == 1 && list.contains(cgmes));
        list = caseInfosService.searchCaseInfos(CaseInfosService.getDateSearchTerm(cgmes.getDate()) + " AND tso:(ES OR FR OR PT) AND businessProcess:(2D) AND format:CGMES");
        assertTrue(list.size() == 1 && list.contains(cgmes));

        list = caseInfosService.searchCaseInfos("geographicalCode:(D4 OR D8)");
        assertTrue(list.size() == 2 && list.contains(ucte1) && list.contains(ucte6));

        list = caseInfosService.searchCaseInfos(CaseInfosService.getDateSearchTerm(ucte6.getDate()) + " OR " + CaseInfosService.getDateSearchTerm(cgmes.getDate()));
        assertTrue(list.size() == 2 && list.contains(ucte6) && list.contains(cgmes));

        list = caseInfosService.searchCaseInfos(CaseInfosService.getDateSearchTerm(ucte6.getDate(), cgmes.getDate()));
        assertTrue(list.size() == 2 && list.contains(ucte6) && list.contains(cgmes));

        list = caseInfosService.searchCaseInfos(CaseInfosService.getDateSearchTerm(ucte1.getDate()) + " AND geographicalCode:D8 AND forecastDistance:0");
        assertTrue(list.size() == 1 && list.contains(ucte1));
    }

    private void testEquals(CaseInfos c1, CaseInfos c2) {
        new EqualsTester()
                .addEqualityGroup(c1, c2)
                .testEquals();
    }

    private CaseInfos createInfos(String fileName) {
        Path casePath = Path.of(this.getClass().getResource("/" + fileName).getPath());
        String fileBaseName = casePath.getFileName().toString();
        String format = caseService.getFormat(casePath);
        return caseService.createInfos(fileBaseName, UUID.randomUUID(), format);
    }
}
