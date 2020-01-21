/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver.datasource.util;

import com.powsybl.caseserver.CaseService;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.exceptions.UncheckedUnsupportedEncodingException;
import com.powsybl.iidm.import_.Importers;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.util.Set;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Service
@ComponentScan(basePackageClasses = CaseService.class)
public class CaseDataSourceService {

    @Autowired
    private CaseService caseService;

    String getBaseName(String caseName) {
        DataSource dataSource = getDatasource(caseName);
        return dataSource.getBaseName();
    }

    Boolean datasourceExists(String caseName, String suffix, String ext) {
        DataSource dataSource = getDatasource(caseName);
        try {
            return dataSource.exists(suffix, ext);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    Boolean datasourceExists(String caseName, String fileName) {
        DataSource dataSource = getDatasource(caseName);
        try {
            return dataSource.exists(fileName);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    byte[] getInputStream(String caseName, String fileName) {
        DataSource dataSource = getDatasource(caseName);
        try (InputStream inputStream = dataSource.newInputStream(fileName)) {
            return IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    byte[] getInputStream(String caseName, String suffix, String ext) {
        DataSource dataSource = getDatasource(caseName);
        try (InputStream inputStream = dataSource.newInputStream(suffix, ext)) {
            return IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    Set<String> listName(String caseName, String regex) {
        DataSource dataSource = getDatasource(caseName);
        String decodedRegex;
        try {
            decodedRegex = URLDecoder.decode(regex, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new UncheckedUnsupportedEncodingException(e);
        }
        try {
            return dataSource.listNames(decodedRegex);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private DataSource initDatasource(String caseName) {
        Path file = getStorageRootDir().resolve(caseName);
        return Importers.createDataSource(file);
    }

    private Path getStorageRootDir() {
        return caseService.getStorageRootDir();
    }

    private DataSource getDatasource(String caseName) {
        caseService.checkStorageInitialization();
        return initDatasource(caseName);
    }

}

