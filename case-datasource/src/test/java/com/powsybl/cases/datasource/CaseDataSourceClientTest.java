/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cases.datasource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */

@RunWith(MockitoJUnitRunner.class)
public class CaseDataSourceClientTest {

    @Mock
    private RestTemplate caseServerRest;

    private CaseDataSourceClient caseDataSourceClient;

    private UUID randomUuid = UUID.randomUUID();

    @Before
    public void setUp() {
        caseDataSourceClient = new CaseDataSourceClient(caseServerRest, randomUuid);

        given(caseServerRest.exchange(eq("/v1/cases/{caseUuid}/datasource/baseName"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class),
                eq(randomUuid)))
                .willReturn(ResponseEntity.ok("myCaseName"));

        ParameterizedTypeReference< Set<String>> parameterizedTypeReference = new ParameterizedTypeReference<Set<String>>() { };

        given(caseServerRest.exchange(eq("/v1/cases/" + randomUuid + "/datasource/list?regex=.*"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(parameterizedTypeReference)))
                .willReturn(ResponseEntity.ok(new HashSet<>(asList("A.xml", "B.xml"))));

        given(caseServerRest.exchange(eq("/v1/cases/" + randomUuid + "/datasource?fileName=A.xml"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(byte[].class)))
                .willReturn(ResponseEntity.ok("Data in the file".getBytes()));

        given(caseServerRest.exchange(eq("/v1/cases/" +  randomUuid + "/datasource?suffix=A&ext=xml"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(byte[].class)))
                .willReturn(ResponseEntity.ok("Data in the file".getBytes()));

        given(caseServerRest.exchange(eq("/v1/cases/"  + randomUuid + "/datasource/exists?fileName=A.xml"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Boolean.class)))
                .willReturn(ResponseEntity.ok(true));

        given(caseServerRest.exchange(eq("/v1/cases/"  + randomUuid + "/datasource/exists?suffix=A&ext=xml"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Boolean.class)))
                .willReturn(ResponseEntity.ok(true));
    }

    @Test
    public void test() throws IOException {
        assertEquals("myCaseName", caseDataSourceClient.getBaseName());

        assertEquals(new HashSet<>(asList("A.xml", "B.xml")), caseDataSourceClient.listNames(".*"));

        assertTrue(caseDataSourceClient.exists("A.xml"));
        assertTrue(caseDataSourceClient.exists("A", "xml"));

        try (InputStreamReader isReader = new InputStreamReader(caseDataSourceClient.newInputStream("A.xml"), StandardCharsets.UTF_8)) {
            BufferedReader reader = new BufferedReader(isReader);
            StringBuilder datasourceResponse = new StringBuilder();
            String str;
            while ((str = reader.readLine()) != null) {
                datasourceResponse.append(str);
            }
            assertEquals("Data in the file", datasourceResponse.toString());
        }

        try (InputStreamReader isReader = new InputStreamReader(caseDataSourceClient.newInputStream("A", "xml"), StandardCharsets.UTF_8)) {
            BufferedReader reader = new BufferedReader(isReader);
            StringBuilder datasourceResponse = new StringBuilder();
            String str;
            while ((str = reader.readLine()) != null) {
                datasourceResponse.append(str);
            }
            assertEquals("Data in the file", datasourceResponse.toString());
        }
    }
}
