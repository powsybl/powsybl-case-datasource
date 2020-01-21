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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
public class CaseDataSourceClient implements ReadOnlyDataSource {

    private static final String CASE_API_VERSION = "v1";
    private static final String CASE_NAME = "caseName";

    private RestTemplate caseServerRest;
    private String caseServerBaseUri;

    private String caseName;

    public CaseDataSourceClient(@Value("${case-server.base.url}") String caseServerBaseUri, String caseName) {
        this(caseServerBaseUri);
        this.caseName = Objects.requireNonNull(caseName);
    }

    public CaseDataSourceClient(@Value("${case-server.base.url}") String caseServerBaseUri) {
        this.caseServerBaseUri = Objects.requireNonNull(caseServerBaseUri);
        RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
        this.caseServerRest = restTemplateBuilder.build();
        this.caseServerRest.setUriTemplateHandler(new DefaultUriBuilderFactory(caseServerBaseUri));
    }

    @Override
    public String getBaseName() {
        HttpHeaders requestHeaders = new HttpHeaders();
        HttpEntity requestEntity = new HttpEntity(requestHeaders);

        Map<String, Object> urlParams = new HashMap<>();
        urlParams.put(CASE_NAME, caseName);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(caseServerBaseUri + "/" + CASE_API_VERSION + "/cases/{caseName}/datasource/baseName")
                .uriVariables(urlParams);
        try {
            ResponseEntity<String> responseEntity = caseServerRest.exchange(uriBuilder.toUriString(),
                    HttpMethod.GET,
                    requestEntity,
                    String.class);
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            throw new CaseDataSourceClientException("Exception when requesting baseName", e);
        }
    }

    @Override
    public boolean exists(String suffix, String ext) {
        HttpHeaders requestHeaders = new HttpHeaders();
        HttpEntity requestEntity = new HttpEntity(requestHeaders);

        Map<String, Object> urlParams = new HashMap<>();
        urlParams.put(CASE_NAME, caseName);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(caseServerBaseUri + "/" + CASE_API_VERSION + "/cases/{caseName}/datasource/exists")
                .uriVariables(urlParams)
                .queryParam("suffix", suffix)
                .queryParam("ext", ext);
        try {
            ResponseEntity<Boolean> responseEntity = caseServerRest.exchange(uriBuilder.toUriString(),
                    HttpMethod.GET,
                    requestEntity,
                    Boolean.class);
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            throw new CaseDataSourceClientException("Exception when checking file existence: " + e.getResponseBodyAsString());
        }
    }

    @Override
    public boolean exists(String fileName) {
        HttpHeaders requestHeaders = new HttpHeaders();
        HttpEntity requestEntity = new HttpEntity(requestHeaders);

        Map<String, Object> urlParams = new HashMap<>();
        urlParams.put(CASE_NAME, caseName);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(caseServerBaseUri + "/" + CASE_API_VERSION + "/cases/{caseName}/datasource/exists")
                .uriVariables(urlParams)
                .queryParam("fileName", fileName);
        try {
            ResponseEntity<Boolean> responseEntity = caseServerRest.exchange(uriBuilder.toUriString(),
                    HttpMethod.GET,
                    requestEntity,
                    Boolean.class);
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            throw new CaseDataSourceClientException("Exception when checking file existence:" + e.getResponseBodyAsString());
        }
    }

    @Override
    public InputStream newInputStream(String suffix, String ext) throws IOException {
        HttpHeaders requestHeaders = new HttpHeaders();
        HttpEntity requestEntity = new HttpEntity(requestHeaders);

        Map<String, Object> urlParams = new HashMap<>();
        urlParams.put(CASE_NAME, caseName);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(caseServerBaseUri + "/" + CASE_API_VERSION + "/cases/{caseName}/datasource")
                .uriVariables(urlParams)
                .queryParam("suffix", suffix)
                .queryParam("ext", ext);
        try {
            ResponseEntity<byte[]> responseEntity = caseServerRest.exchange(uriBuilder.toUriString(),
                    HttpMethod.GET,
                    requestEntity,
                    byte[].class);
            return new ByteArrayInputStream(responseEntity.getBody());
        } catch (HttpStatusCodeException e) {
            throw new CaseDataSourceClientException("Exception when requesting the file inputStream: " + e.getResponseBodyAsString());
        }
    }

    @Override
    public InputStream newInputStream(String fileName) {
        HttpHeaders requestHeaders = new HttpHeaders();
        HttpEntity requestEntity = new HttpEntity(requestHeaders);

        Map<String, Object> urlParams = new HashMap<>();
        urlParams.put(CASE_NAME, caseName);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(caseServerBaseUri + "/" + CASE_API_VERSION + "/cases/{caseName}/datasource")
                .uriVariables(urlParams)
                .queryParam("fileName", fileName);
        try {
            ResponseEntity<byte[]> responseEntity = caseServerRest.exchange(uriBuilder.toUriString(),
                    HttpMethod.GET,
                    requestEntity,
                    byte[].class);
            return new ByteArrayInputStream(responseEntity.getBody());
        } catch (HttpStatusCodeException e) {
            throw new CaseDataSourceClientException("Exception when requesting the file inputStream: " + e.getResponseBodyAsString());
        }
    }

    @Override
    public Set<String> listNames(String regex) {
        HttpHeaders requestHeaders = new HttpHeaders();
        HttpEntity requestEntity = new HttpEntity(requestHeaders);

        Map<String, Object> urlParams = new HashMap<>();
        urlParams.put(CASE_NAME, caseName);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(caseServerBaseUri + "/" + CASE_API_VERSION + "/cases/{caseName}/datasource/list")
                .uriVariables(urlParams)
                .queryParam(CASE_NAME, caseName)
                .queryParam("regex", regex);
        try {
            ResponseEntity<Set<String>> responseEntity = caseServerRest.exchange(uriBuilder.toUriString(),
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<Set<String>>() { });
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            throw new CaseDataSourceClientException("Exception when requesting the files listNames: " + e.getResponseBodyAsString());
        }
    }

    void setCaseName(String caseName) {
        this.caseName = caseName;
    }
}
