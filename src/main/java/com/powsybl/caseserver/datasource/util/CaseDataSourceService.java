package com.powsybl.caseserver.datasource.util;

import com.powsybl.caseserver.CaseException;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.exceptions.UncheckedUnsupportedEncodingException;
import com.powsybl.iidm.import_.Importers;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URLDecoder;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Set;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Service
class CaseDataSourceService {

    private FileSystem fileSystem = FileSystems.getDefault();
    @Value("${case-store-directory:#{systemProperties['user.home'].concat(\"/cases\")}}")
    private String rootDirectory;

    String getBaseName(String caseName) {
        DataSource dataSource = initDatasource(caseName);
        return dataSource.getBaseName();
    }

    Boolean datasourceExists(String caseName, String suffix, String ext) {
        DataSource dataSource = initDatasource(caseName);
        try {
            return dataSource.exists(suffix, ext);
        } catch (IOException e) {
            throw new CaseException(e.getMessage());
        }
    }

    Boolean datasourceExists(String caseName, String fileName) {
        DataSource dataSource = initDatasource(caseName);
        try {
            return dataSource.exists(fileName);
        } catch (IOException e) {
            throw new CaseException(e.getMessage());
        }
    }

    byte[] getInputStream(String caseName, String fileName) {
        DataSource dataSource = initDatasource(caseName);
        try (InputStream inputStream = dataSource.newInputStream(fileName)) {
            return IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    byte[] getInputStream(String caseName, String suffix, String ext) {
        DataSource dataSource = initDatasource(caseName);
        try (InputStream inputStream = dataSource.newInputStream(suffix, ext)) {
            return IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    Set<String> listName(String caseName, String regex) {
        String decodedRegex;
        try {
            decodedRegex = URLDecoder.decode(regex, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new UncheckedUnsupportedEncodingException(e);
        }
        DataSource dataSource = initDatasource(caseName);
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
        return fileSystem.getPath(rootDirectory);
    }
}
