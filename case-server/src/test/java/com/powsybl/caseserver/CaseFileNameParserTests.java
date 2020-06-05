/*
  Copyright (c) 2020, RTE (http://www.rte-france.com)
  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver;

import com.powsybl.caseserver.entsoe.EntsoeCaseInfos;
import com.powsybl.caseserver.parsers.entsoe.EntsoeFileNameParser;
import com.powsybl.entsoe.util.EntsoeGeographicalCode;
import com.powsybl.iidm.network.Country;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * @author Slimane Amar <slimane.amar at rte-france.com>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(classes = {CaseController.class})
class CaseFileNameParserTests {

    private static final String SN_UCTE_CASE_FILE_NAME      = "20200103_0915_SN5_D80.UCT";
    private static final String ID_UCTE_CASE_FILE_NAME      = "20200424_1330_135_CH2.UCT";
    private static final String D1_UCTE_CASE_FILE_NAME      = "20200110_0430_FO5_FR0.uct";
    private static final String D2_UCTE_CASE_FILE_NAME      = "20200430_1530_2D4_D41.uct";
    private static final String TEST_CGMES_CASE_FILE_NAME   = "20200212_1030_FO3_FR1.zip";
    private static final String TEST_OTHER_CASE_FILE_NAME   = "testCase.xiidm";

    @Autowired
    private CaseService caseService;

    @Test
    void testValidNameUcteSN() {
        EntsoeCaseInfos caseInfos = (EntsoeCaseInfos) createInfos(SN_UCTE_CASE_FILE_NAME);
        assertEquals(SN_UCTE_CASE_FILE_NAME, caseInfos.getName());
        assertEquals("UCTE", caseInfos.getFormat());
        assertTrue(caseInfos.getDate().isEqual(EntsoeFileNameParser.parseDateTime(SN_UCTE_CASE_FILE_NAME.substring(0, 13))));
        assertEquals(Integer.valueOf(0), caseInfos.getForecastDistance());
        assertSame(EntsoeGeographicalCode.D8, caseInfos.getGeographicalCode());
        assertSame(Country.DE, caseInfos.getCountry());
        assertEquals(Integer.valueOf(0), caseInfos.getVersion());
    }

    @Test
    void testValidNameUcteID() {
        EntsoeCaseInfos caseInfos = (EntsoeCaseInfos) createInfos(ID_UCTE_CASE_FILE_NAME);
        assertEquals(ID_UCTE_CASE_FILE_NAME, caseInfos.getName());
        assertEquals("UCTE", caseInfos.getFormat());
        assertTrue(caseInfos.getDate().isEqual(EntsoeFileNameParser.parseDateTime(ID_UCTE_CASE_FILE_NAME.substring(0, 13))));
        assertEquals(Integer.valueOf(780), caseInfos.getForecastDistance());
        assertSame(EntsoeGeographicalCode.CH, caseInfos.getGeographicalCode());
        assertSame(Country.CH, caseInfos.getCountry());
        assertEquals(Integer.valueOf(2), caseInfos.getVersion());
    }

    @Test
    void testValidNameUcte1D() {
        EntsoeCaseInfos caseInfos = (EntsoeCaseInfos) createInfos(D1_UCTE_CASE_FILE_NAME);
        assertEquals(D1_UCTE_CASE_FILE_NAME, caseInfos.getName());
        assertEquals("UCTE", caseInfos.getFormat());
        assertTrue(caseInfos.getDate().isEqual(EntsoeFileNameParser.parseDateTime(D1_UCTE_CASE_FILE_NAME.substring(0, 13))));
        assertEquals(Integer.valueOf(630), caseInfos.getForecastDistance());
        assertSame(EntsoeGeographicalCode.FR, caseInfos.getGeographicalCode());
        assertSame(Country.FR, caseInfos.getCountry());
        assertEquals(Integer.valueOf(0), caseInfos.getVersion());
    }

    @Test
    void testValidNameUcte2D() {
        EntsoeCaseInfos caseInfos = (EntsoeCaseInfos) createInfos(D2_UCTE_CASE_FILE_NAME);
        assertEquals(D2_UCTE_CASE_FILE_NAME, caseInfos.getName());
        assertEquals("UCTE", caseInfos.getFormat());
        assertTrue(caseInfos.getDate().isEqual(EntsoeFileNameParser.parseDateTime(D2_UCTE_CASE_FILE_NAME.substring(0, 13))));
        assertEquals(Integer.valueOf(2730), caseInfos.getForecastDistance());
        assertSame(EntsoeGeographicalCode.D4, caseInfos.getGeographicalCode());
        assertSame(Country.DE, caseInfos.getCountry());
        assertEquals(Integer.valueOf(1), caseInfos.getVersion());
    }

    @Test
    void testValidNameCgmes() {
        EntsoeCaseInfos caseInfos = (EntsoeCaseInfos) createInfos(TEST_CGMES_CASE_FILE_NAME);
        assertEquals(TEST_CGMES_CASE_FILE_NAME, caseInfos.getName());
        assertEquals("CGMES", caseInfos.getFormat());
        assertTrue(caseInfos.getDate().isEqual(EntsoeFileNameParser.parseDateTime(TEST_CGMES_CASE_FILE_NAME.substring(0, 13))));
        assertEquals(Integer.valueOf(990), caseInfos.getForecastDistance());
        assertSame(EntsoeGeographicalCode.FR, caseInfos.getGeographicalCode());
        assertSame(Country.FR, caseInfos.getCountry());
        assertEquals(Integer.valueOf(1), caseInfos.getVersion());
    }

    @Test
    void testNonValidNameEntsoe() {
        CaseInfos caseInfos = createInfos(TEST_OTHER_CASE_FILE_NAME);
        assertEquals(TEST_OTHER_CASE_FILE_NAME, caseInfos.getName());
        assertEquals("XIIDM", caseInfos.getFormat());
    }

    private CaseInfos createInfos(String fileName) {
        Path casePath = Path.of(this.getClass().getResource("/" + fileName).getPath());
        String fileBaseName = casePath.getFileName().toString();
        String format = caseService.getFormat(casePath);
        return caseService.createInfos(fileBaseName, UUID.randomUUID(), format);
    }
}
