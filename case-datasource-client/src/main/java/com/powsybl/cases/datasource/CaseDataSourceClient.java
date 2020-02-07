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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;
import java.util.Set;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
public class CaseDataSourceClient implements ReadOnlyDataSource {

    private static final String CASE_API_VERSION = "v1";
    private static final String CASE_NAME = "caseName";

    private final RestTemplate restTemplate;

    private String caseName;

    public CaseDataSourceClient(RestTemplate restTemplate, String caseName) {
        this.restTemplate = Objects.requireNonNull(restTemplate);
        this.caseName = Objects.requireNonNull(caseName);
    }

    private static RestTemplate createRestTemplate(String caseServerBaseUri) {
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(caseServerBaseUri));
        return restTemplate;
    }

    public CaseDataSourceClient(@Value("${case-server.base.url}") String caseServerBaseUri, String caseName) {
        this(createRestTemplate(caseServerBaseUri), caseName);
    }

    @Override
    public String getBaseName() {
        try {
            ResponseEntity<String> responseEntity = restTemplate.exchange("/" + CASE_API_VERSION + "/cases/{caseName}/datasource/baseName",
                                                                          HttpMethod.GET,
                                                                          HttpEntity.EMPTY,
                                                                          String.class,
                                                                          caseName);
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            throw new CaseDataSourceClientException("Exception when requesting baseName", e);
        }
    }

    @Override
    public boolean exists(String suffix, String ext) {
        String path = UriComponentsBuilder.fromPath("/" + CASE_API_VERSION + "/cases/{caseName}/datasource/exists")
                .queryParam("suffix", suffix)
                .queryParam("ext", ext)
                .buildAndExpand(caseName)
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
        String path = UriComponentsBuilder.fromPath("/" + CASE_API_VERSION + "/cases/{caseName}/datasource/exists")
                .queryParam("fileName", fileName)
                .buildAndExpand(caseName)
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
    public InputStream newInputStream(String suffix, String ext) {
        String path = UriComponentsBuilder.fromPath("/" + CASE_API_VERSION + "/cases/{caseName}/datasource")
                .queryParam("suffix", suffix)
                .queryParam("ext", ext)
                .buildAndExpand(caseName)
                .toUriString();
        try {
            ResponseEntity<byte[]> responseEntity = restTemplate.exchange(path,
                                                                          HttpMethod.GET,
                                                                          HttpEntity.EMPTY,
                                                                          byte[].class);
            return new ByteArrayInputStream(responseEntity.getBody());
        } catch (HttpStatusCodeException e) {
            throw new CaseDataSourceClientException("Exception when requesting the file inputStream: " + e.getResponseBodyAsString());
        }
    }

    @Override
    public InputStream newInputStream(String fileName) {
        String path = UriComponentsBuilder.fromPath("/" + CASE_API_VERSION + "/cases/{caseName}/datasource")
                .queryParam("fileName", fileName)
                .buildAndExpand(caseName)
                .toUriString();
        try {
            ResponseEntity<byte[]> responseEntity = restTemplate.exchange(path,
                                                                          HttpMethod.GET,
                                                                          HttpEntity.EMPTY,
                                                                          byte[].class);
            return new ByteArrayInputStream(responseEntity.getBody());
        } catch (HttpStatusCodeException e) {
            throw new CaseDataSourceClientException("Exception when requesting the file inputStream: " + e.getResponseBodyAsString());
        }
    }

    @Override
    public Set<String> listNames(String regex) {
        String path = UriComponentsBuilder.fromPath("/" + CASE_API_VERSION + "/cases/{caseName}/datasource/list")
                .queryParam(CASE_NAME, caseName)
                .queryParam("regex", regex)
                .buildAndExpand(caseName)
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

    public void setCaseName(String caseName) {
        this.caseName = Objects.requireNonNull(caseName);
    }
}
