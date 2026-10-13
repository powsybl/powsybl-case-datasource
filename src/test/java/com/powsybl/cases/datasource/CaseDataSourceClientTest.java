/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cases.datasource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.UUID;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@ExtendWith(MockitoExtension.class)
class CaseDataSourceClientTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private RestClient caseServerRest;

    @Mock
    private Resource resource;

    private CaseDataSourceClient caseDataSourceClient;

    private UUID caseUuid;

    @BeforeEach
    void setUp() {
        caseUuid = UUID.randomUUID();
        caseDataSourceClient = new CaseDataSourceClient(caseServerRest, caseUuid);
    }

    @Test
    void getBaseName() {
        given(caseServerRest.get()
                .uri(eq("/v1/cases/{caseUuid}/datasource/baseName"), eq(caseUuid))
                .retrieve()
                .toEntity(String.class))
                .willReturn(ResponseEntity.ok("myCaseName"));

        assertEquals("myCaseName", caseDataSourceClient.getBaseName());
    }

    @Test
    void listName() {
        given(caseServerRest.get()
                .uri(eq("/v1/cases/" + caseUuid + "/datasource/list?regex=.*"))
                .retrieve()
                .toEntity(any(ParameterizedTypeReference.class)))
                .willReturn(ResponseEntity.ok(new HashSet<>(asList("A.xml", "B.xml"))));

        assertEquals(new HashSet<>(asList("A.xml", "B.xml")), caseDataSourceClient.listNames(".*"));
    }

    @Test
    void exists() {
        given(caseServerRest.get()
                .uri(eq("/v1/cases/" + caseUuid + "/datasource/exists?fileName=A.xml"))
                .retrieve()
                .toEntity(Boolean.class))
                .willReturn(ResponseEntity.ok(true));

        assertTrue(caseDataSourceClient.exists("A.xml"));

        given(caseServerRest.get()
                .uri(eq("/v1/cases/" + caseUuid + "/datasource/exists?suffix=A&ext=xml"))
                .retrieve()
                .toEntity(Boolean.class))
                .willReturn(ResponseEntity.ok(true));

        assertTrue(caseDataSourceClient.exists("A", "xml"));
    }

    @Test
    void newInputStream() throws IOException {
        String data = "Data in the file";
        byte[] responseBytes = data.getBytes();

        given(caseServerRest.get()
                .uri(eq("/v1/cases/" + caseUuid + "/datasource?fileName=A.xml"))
                .retrieve()
                .toEntity(Resource.class))
                .willReturn(ResponseEntity.ok(new InputStreamResource(new ByteArrayInputStream(responseBytes))));

        try (InputStreamReader isReader = new InputStreamReader(caseDataSourceClient.newInputStream("A.xml"), StandardCharsets.UTF_8)) {
            BufferedReader reader = new BufferedReader(isReader);
            StringBuilder datasourceResponse = new StringBuilder();
            String str;
            while ((str = reader.readLine()) != null) {
                datasourceResponse.append(str);
            }
            assertEquals("Data in the file", datasourceResponse.toString());
        }

        given(caseServerRest.get()
            .uri(eq("/v1/cases/" + caseUuid + "/datasource?suffix=A&ext=xml"))
            .retrieve()
            .toEntity(Resource.class))
            .willReturn(ResponseEntity.ok(new InputStreamResource(new ByteArrayInputStream(responseBytes))));

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

    @Test
    void newInputStreamWithEmptyBody() {
        given(caseServerRest.get()
                .uri(eq("/v1/cases/" + caseUuid + "/datasource?fileName=A.xml"))
                .retrieve()
                .toEntity(Resource.class))
                .willReturn(ResponseEntity.ok().build());

        CaseDataSourceClientException exception = assertThrows(CaseDataSourceClientException.class, () -> caseDataSourceClient.newInputStream("A.xml"));
        assertTrue(exception.getMessage().contains("Response body is null for"));

        given(caseServerRest.get()
                .uri(eq("/v1/cases/" + caseUuid + "/datasource?suffix=A&ext=xml"))
                .retrieve()
                .toEntity(Resource.class))
                .willReturn(ResponseEntity.ok().build());

        exception = assertThrows(CaseDataSourceClientException.class, () -> caseDataSourceClient.newInputStream("A", "xml"));
        assertTrue(exception.getMessage().contains("Response body is null for"));
    }

    @Test
    void newInputStreamWithIOException() throws IOException {
        given(resource.getInputStream()).willThrow(new IOException("Test"));

        given(caseServerRest.get()
                .uri(eq("/v1/cases/" + caseUuid + "/datasource?fileName=A.xml"))
                .retrieve()
                .toEntity(Resource.class))
                .willReturn(ResponseEntity.ok(resource));

        CaseDataSourceClientException exception = assertThrows(CaseDataSourceClientException.class, () -> caseDataSourceClient.newInputStream("A.xml"));
        assertTrue(exception.getMessage().contains("I/O error when opening inputStream for"));

        given(caseServerRest.get()
                .uri(eq("/v1/cases/" + caseUuid + "/datasource?suffix=A&ext=xml"))
                .retrieve()
                .toEntity(Resource.class))
                .willReturn(ResponseEntity.ok(resource));

        exception = assertThrows(CaseDataSourceClientException.class, () -> caseDataSourceClient.newInputStream("A", "xml"));
        assertTrue(exception.getMessage().contains("I/O error when opening inputStream for"));
    }
}
