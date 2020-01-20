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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

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

    @InjectMocks
    CaseDataSourceClient caseDataSourceClient = new CaseDataSourceClient("http://localhost:8080");

    @Before
    public void setUp() {

        ResponseEntity<String> myEntity = new ResponseEntity<>(HttpStatus.ACCEPTED);

        given(caseServerRest.exchange(eq("http://localhost:8080/v1/cases/myCaseName.zip/datasource/baseName"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)))
                .willReturn(ResponseEntity.ok("myCaseName"));

        ParameterizedTypeReference< Set<String>> parameterizedTypeReference = new ParameterizedTypeReference<Set<String>>() { };

        given(caseServerRest.exchange(eq("http://localhost:8080/v1/cases/myCaseName.zip/datasource/list?caseName=myCaseName.zip&regex=.*"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(parameterizedTypeReference)))
                .willReturn(ResponseEntity.ok(new HashSet<>(asList("A.xml", "B.xml"))));

        given(caseServerRest.exchange(eq("http://localhost:8080/v1/cases/myCaseName.zip/datasource?fileName=A.xml"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(byte[].class)))
                .willReturn(ResponseEntity.ok("Data in the file".getBytes()));

        given(caseServerRest.exchange(eq("http://localhost:8080/v1/cases/myCaseName.zip/datasource/exists?fileName=A.xml"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Boolean.class)))
                .willReturn(ResponseEntity.ok(true));

        given(caseServerRest.exchange(eq("http://localhost:8080/v1/cases/myCaseName.zip/datasource/exists?suffix=A&ext=xml"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Boolean.class)))
                .willReturn(ResponseEntity.ok(true));
    }

    @Test
    public void test() throws IOException {
        caseDataSourceClient.setCaseName("myCaseName.zip");
        assertEquals("myCaseName", caseDataSourceClient.getBaseName());

        assertEquals(new HashSet<>(asList("A.xml", "B.xml")), caseDataSourceClient.listNames(".*"));

        assertTrue(caseDataSourceClient.exists("A.xml"));
        assertTrue(caseDataSourceClient.exists("A", "xml"));

        byte[] buffer = new byte[16];
        caseDataSourceClient.newInputStream("A.xml").read(buffer);
        assertEquals("Data in the file", new String(buffer));
    }
}
