/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver.datasource.util;

import com.powsybl.caseserver.CaseService;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.iidm.network.Importers;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Service
@ComponentScan(basePackageClasses = CaseService.class)
public class CaseDataSourceService {

    @Autowired
    private CaseService caseService;

    String getBaseName(UUID caseUuid) {
        DataSource dataSource = getDatasource(caseUuid);
        return dataSource.getBaseName();
    }

    Boolean datasourceExists(UUID caseUuid, String suffix, String ext) {
        DataSource dataSource = getDatasource(caseUuid);
        try {
            return dataSource.exists(suffix, ext);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    Boolean datasourceExists(UUID caseUuid, String fileName) {
        DataSource dataSource = getDatasource(caseUuid);
        try {
            return dataSource.exists(fileName);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    byte[] getInputStream(UUID caseUuid, String fileName) {
        DataSource dataSource = getDatasource(caseUuid);
        try (InputStream inputStream = dataSource.newInputStream(fileName)) {
            return IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    byte[] getInputStream(UUID caseUuid, String suffix, String ext) {
        DataSource dataSource = getDatasource(caseUuid);
        try (InputStream inputStream = dataSource.newInputStream(suffix, ext)) {
            return IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    Set<String> listName(UUID caseUuid, String regex) {
        DataSource dataSource = getDatasource(caseUuid);
        String decodedRegex = URLDecoder.decode(regex, StandardCharsets.UTF_8);
        try {
            return dataSource.listNames(decodedRegex);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private DataSource initDatasource(UUID caseUuid) {
        Path file = caseService.getCaseFile(caseUuid);
        return Importers.createDataSource(file);
    }

    private DataSource getDatasource(UUID caseUuid) {
        caseService.checkStorageInitialization();
        return initDatasource(caseUuid);
    }

}

