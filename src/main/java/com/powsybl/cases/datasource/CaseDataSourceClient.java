/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cases.datasource;

import com.powsybl.commons.datasource.ReadOnlyDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
public class CaseDataSourceClient implements ReadOnlyDataSource {

    private static final String CASE_API_VERSION = "v1";

    private final RestTemplate restTemplate;

    private UUID caseUuid;

    public CaseDataSourceClient(RestTemplate restTemplate, UUID caseUuid) {
        this.restTemplate = Objects.requireNonNull(restTemplate);
        this.caseUuid = Objects.requireNonNull(caseUuid);
    }

    private static RestTemplate createRestTemplate(String caseServerBaseUri) {
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(caseServerBaseUri));
        return restTemplate;
    }

    public CaseDataSourceClient(@Value("${case-server.base.url:http://case-server/}") String caseServerBaseUri, UUID caseUuid) {
        this(createRestTemplate(caseServerBaseUri), caseUuid);
    }

    @Override
    public String getBaseName() {
        try {
            ResponseEntity<String> responseEntity = restTemplate.exchange("/" + CASE_API_VERSION + "/cases/{caseUuid}/datasource/baseName",
                                                                          HttpMethod.GET,
                                                                          HttpEntity.EMPTY,
                                                                          String.class,
                                                                          caseUuid);
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            throw new CaseDataSourceClientException("Exception when requesting baseName", e);
        }
    }

    @Override
    public boolean exists(String suffix, String ext) {
        String path = UriComponentsBuilder.fromPath("/" + CASE_API_VERSION + "/cases/{caseUuid}/datasource/exists")
                .queryParam("suffix", suffix)
                .queryParam("ext", ext)
                .buildAndExpand(caseUuid)
                .toUriString();
        try {
            ResponseEntity<Boolean> responseEntity = restTemplate.exchange(path,
                                                                           HttpMethod.GET,
                                                                           HttpEntity.EMPTY,
                                                                           Boolean.class);
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            throw new CaseDataSourceClientException("Exception when checking file existence: " + e.getResponseBodyAsString());
        }
    }

    @Override
    public boolean exists(String fileName) {
        String path = UriComponentsBuilder.fromPath("/" + CASE_API_VERSION + "/cases/{caseUuid}/datasource/exists")
                .queryParam("fileName", fileName)
                .buildAndExpand(caseUuid)
                .toUriString();
        try {
            ResponseEntity<Boolean> responseEntity = restTemplate.exchange(path,
                                                                           HttpMethod.GET,
                                                                           HttpEntity.EMPTY,
                                                                           Boolean.class);
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            throw new CaseDataSourceClientException("Exception when checking file existence:" + e.getResponseBodyAsString());
        }
    }

    @Override
    public boolean isDataExtension(String s) {
        return true;
    }

    @Override
    public InputStream newInputStream(String suffix, String ext) {
        return fetchInputStream(UriComponentsBuilder.fromPath("/" + CASE_API_VERSION + "/cases/{caseUuid}/datasource")
                        .queryParam("suffix", suffix)
                        .queryParam("ext", ext)
                        .buildAndExpand(caseUuid)
                        .toUriString(),
                        "suffix: " + suffix + ", ext: " + ext);
    }

    @Override
    public InputStream newInputStream(String fileName) {
        return fetchInputStream(UriComponentsBuilder.fromPath("/" + CASE_API_VERSION + "/cases/{caseUuid}/datasource")
                        .queryParam("fileName", fileName)
                        .buildAndExpand(caseUuid)
                        .toUriString(),
                        "fileName: " + fileName);
    }

    private InputStream fetchInputStream(String path, String description) {
        try {
            ResponseEntity<Resource> responseEntity = restTemplate.exchange(path, HttpMethod.GET, HttpEntity.EMPTY, Resource.class);
            Resource body = responseEntity.getBody();
            if (body == null) {
                throw new CaseDataSourceClientException("Response body is null for " + description);
            }
            return body.getInputStream();
        } catch (HttpStatusCodeException e) {
            throw new CaseDataSourceClientException("HTTP error when requesting file inputStream for " + description + ": " + e.getResponseBodyAsString(), e);
        } catch (IOException e) {
            throw new CaseDataSourceClientException("I/O error when opening inputStream for " + description, e);
        }
    }

    @Override
    public Set<String> listNames(String regex) {
        String path = UriComponentsBuilder.fromPath("/" + CASE_API_VERSION + "/cases/{caseUuid}/datasource/list")
                .queryParam("regex", regex)
                .buildAndExpand(caseUuid)
                .toUriString();
        try {
            ResponseEntity<Set<String>> responseEntity = restTemplate.exchange(path,
                                                                               HttpMethod.GET,
                                                                               HttpEntity.EMPTY,
                                                                               new ParameterizedTypeReference<Set<String>>() { });
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            throw new CaseDataSourceClientException("Exception when requesting the files listNames: " + e.getResponseBodyAsString());
        }
    }

    public void setCaseName(UUID caseUuid) {
        this.caseUuid = Objects.requireNonNull(caseUuid);
    }
}
