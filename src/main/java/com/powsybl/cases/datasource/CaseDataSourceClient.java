/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cases.datasource;

import com.powsybl.commons.datasource.ReadOnlyDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Component
public class CaseDataSourceClient implements ReadOnlyDataSource, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseDataSourceClient.class);

    private static final String CASE_API_VERSION = "v1";

    private final RestTemplate restTemplate;

    private UUID caseUuid;

    private final Map<String, Path> fileCache = new HashMap<>();

    @Autowired
    public CaseDataSourceClient(RestTemplateBuilder restTemplateBuilder,
                                @Value("${case-server.base.url:http://case-server/}") String caseServerBaseUri,
                                UUID caseUuid) {
        this.restTemplate = Objects.requireNonNull(restTemplateBuilder).uriTemplateHandler(new DefaultUriBuilderFactory(caseServerBaseUri)).build();
        this.caseUuid = Objects.requireNonNull(caseUuid);
    }

    public CaseDataSourceClient(RestTemplate restTemplate, UUID caseUuid) {
        this.restTemplate = Objects.requireNonNull(restTemplate);
        this.caseUuid = Objects.requireNonNull(caseUuid);
    }

    private static RestTemplate createRestTemplate(String caseServerBaseUri) {
        return new RestTemplateBuilder().
                requestFactoryBuilder(ClientHttpRequestFactoryBuilder.simple())
                .uriTemplateHandler(new DefaultUriBuilderFactory(caseServerBaseUri))
                .build();
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
        return checkFileExistence(path);
    }

    @Override
    public boolean exists(String fileName) {
        String path = UriComponentsBuilder.fromPath("/" + CASE_API_VERSION + "/cases/{caseUuid}/datasource/exists")
                .queryParam("fileName", fileName)
                .buildAndExpand(caseUuid)
                .toUriString();
        return checkFileExistence(path);
    }

    private boolean checkFileExistence(String path) {
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
        String path = UriComponentsBuilder.fromPath("/" + CASE_API_VERSION + "/cases/{caseUuid}/datasource")
                .queryParam("suffix", suffix)
                .queryParam("ext", ext)
                .buildAndExpand(caseUuid)
                .toUriString();
        return fetchInputStream(path, suffix + "." + ext);
    }

    @Override
    public InputStream newInputStream(String fileName) {
        String path = UriComponentsBuilder.fromPath("/" + CASE_API_VERSION + "/cases/{caseUuid}/datasource")
                .queryParam("fileName", fileName)
                .buildAndExpand(caseUuid)
                .toUriString();
        return fetchInputStream(path, fileName);
    }

    private InputStream fetchInputStream(String path, String fileName) {
        Path cachedFile = fileCache.get(fileName);
        if (cachedFile != null && Files.exists(cachedFile)) {
            try {
                LOGGER.debug("Loading from cache: {}", path);
                return new FileInputStream(cachedFile.toFile());
            } catch (IOException e) {
                fileCache.remove(fileName);
            }
        }

        try {
            LOGGER.debug("Fetching from case server: {}", path);
            return restTemplate.execute(path, HttpMethod.GET, null, response -> {
                if (response.getStatusCode().is2xxSuccessful()) {
                    Path tempFile = Paths.get("/tmp", "case-datasource-" + caseUuid + "-" + fileName);

                    try (InputStream bodyStream = response.getBody()) {
                        Files.copy(bodyStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        Files.deleteIfExists(tempFile);
                        throw e;
                    }

                    fileCache.put(fileName, tempFile);

                    return new FileInputStream(tempFile.toFile());
                } else {
                    throw new CaseDataSourceClientException("HTTP error " + response.getStatusCode() + " when requesting " + path);
                }
            });
        } catch (HttpStatusCodeException e) {
            throw new CaseDataSourceClientException("Exception when requesting the file inputStream: " + e.getResponseBodyAsString());
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

    @Override
    public void close() {
        for (Path tempFile : fileCache.values()) {
            try {
                LOGGER.debug("Cleaning temporary file: {}", tempFile);
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                LOGGER.error("Failed to delete temporary file: {}", tempFile, e);
            }
        }
        fileCache.clear();
    }
}
