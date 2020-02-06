/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver;

import com.powsybl.commons.datasource.DataSource;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.Importer;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Service
public class CaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseService.class);

    private FileSystem fileSystem = FileSystems.getDefault();

    private ComputationManager computationManager = LocalComputationManager.getDefault();

    @Value("${case-store-directory:#{systemProperties['user.home'].concat(\"/cases\")}}")
    private String rootDirectory;

    private Importer getImporterOrThrowsException(Path caseFile) {
        DataSource dataSource = Importers.createDataSource(caseFile);
        Importer importer = Importers.findImporter(dataSource, computationManager);
        if (importer == null) {
            throw CaseException.createFileNotImportable(caseFile);
        }
        return importer;
    }

    String getFormat(Path caseFile) {
        Importer importer = getImporterOrThrowsException(caseFile);
        return importer.getFormat();
    }

    List<CaseInfos> getCases() {
        checkStorageInitialization();
        try (Stream<Path> walk = Files.walk(getStorageRootDir())) {
            return walk.filter(Files::isRegularFile)
                    .map(file -> new CaseInfos(file.getFileName().toString(), getFormat(file)))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    Path getCaseFile(String caseName) {
        validateCaseName(caseName);
        return getStorageRootDir().resolve(caseName);
    }

    Optional<byte[]> getCaseBytes(String caseName) {
        checkStorageInitialization();

        Path caseFile = getCaseFile(caseName);
        if (Files.exists(caseFile) && Files.isRegularFile(caseFile)) {
            try {
                byte[] bytes = Files.readAllBytes(caseFile);
                return Optional.of(bytes);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return Optional.empty();
    }

    boolean caseExists(String caseName) {
        checkStorageInitialization();

        Path caseFile = getCaseFile(caseName);
        return Files.exists(caseFile) && Files.isRegularFile(caseFile);
    }

    void importCase(MultipartFile mpf) {
        checkStorageInitialization();

        Path caseFile = getCaseFile(mpf.getOriginalFilename());
        if (Files.exists(caseFile) && Files.isRegularFile(caseFile)) {
            throw CaseException.createFileAreadyExists(caseFile);
        }

        try {
            mpf.transferTo(caseFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        try {
            getImporterOrThrowsException(caseFile);
        } catch (CaseException e) {
            try {
                Files.deleteIfExists(caseFile);
            } catch (IOException e2) {
                LOGGER.error(e2.toString(), e2);
            }
            throw e;
        }
    }

    Optional<Network> loadNetwork(String caseName) {
        checkStorageInitialization();

        Path caseFile = getCaseFile(caseName);
        if (Files.exists(caseFile) && Files.isRegularFile(caseFile)) {
            Network network = Importers.loadNetwork(caseFile);
            if (network == null) {
                throw CaseException.createFileNotImportable(caseFile);
            }
            return Optional.of(network);
        }
        return Optional.empty();
    }

    void deleteCase(String caseName) {
        checkStorageInitialization();

        Path caseFile = getCaseFile(caseName);
        if (Files.exists(caseFile) && !Files.isRegularFile(caseFile)) {
            throw CaseException.createFileDoesNotExist(caseFile);
        }

        try {
            Files.delete(caseFile);
        } catch (NoSuchFileException e) {
            throw CaseException.createFileDoesNotExist(caseFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void deleteAllCases() {
        checkStorageInitialization();

        Path caseFolder = getStorageRootDir();

        try (DirectoryStream<Path> paths = Files.newDirectoryStream(caseFolder)) {
            paths.forEach(file -> {
                try {
                    Files.delete(file);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Path getStorageRootDir() {
        return fileSystem.getPath(rootDirectory);
    }

    private boolean isStorageCreated() {
        Path storageRootDir = getStorageRootDir();
        return Files.exists(storageRootDir) && Files.isDirectory(storageRootDir);
    }

    public void checkStorageInitialization() {
        if (!isStorageCreated()) {
            throw CaseException.createStorageNotInitialized(getStorageRootDir());
        }
    }

    public void setFileSystem(FileSystem fileSystem) {
        this.fileSystem = Objects.requireNonNull(fileSystem);
    }

    public void setComputationManager(ComputationManager computationManager) {
        this.computationManager = Objects.requireNonNull(computationManager);
    }

    static void validateCaseName(String caseName) {
        Objects.requireNonNull(caseName);
        if (!caseName.matches("^[\\w0-9\\-]+(\\.[\\w0-9]+)*$")) {
            throw CaseException.createIllegalCaseName(caseName);
        }
    }
}
