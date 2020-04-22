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
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;
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

    private EmitterProcessor<Message<String>> caseInfosPublisher = EmitterProcessor.create();

    @Bean
    public Supplier<Flux<Message<String>>> publishCaseImport() {
        return () -> caseInfosPublisher;
    }

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
        try (Stream<Path> walk = Files.walk(getPublicStorageDir())) {
            return walk.filter(Files::isRegularFile)
                    .map(file -> new CaseInfos(file.getFileName().toString(), getFormat(file), UUID.fromString(file.getParent().getFileName().toString())))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Path getCaseFile(UUID caseUuid) {
        Path publicCaseDirectory = getPublicStorageDir();
        Path privateCaseDirectory = getPrivateStorageDir();
        Path caseFile = walkCaseDirectory(publicCaseDirectory.resolve(caseUuid.toString()));
        if (caseFile != null) {
            return caseFile;
        }
        caseFile = walkCaseDirectory(privateCaseDirectory.resolve(caseUuid.toString()));
        return caseFile;
    }

    Path getCaseDirectory(UUID caseUuid) {
        Path publicCaseDirectory = getPublicStorageDir();
        Path privateCaseDirectory = getPrivateStorageDir();
        Path caseDirectory = publicCaseDirectory.resolve(caseUuid.toString());
        if (Files.exists(caseDirectory) && Files.isDirectory(caseDirectory)) {
            return caseDirectory;
        }
        caseDirectory = privateCaseDirectory.resolve(caseUuid.toString());
        if (Files.exists(caseDirectory) && Files.isDirectory(caseDirectory)) {
            return caseDirectory;
        }
        throw CaseException.createDirectoryNotFound(caseUuid);
    }

    public Path walkCaseDirectory(Path caseDirectory) {
        if (Files.exists(caseDirectory) && Files.isDirectory(caseDirectory)) {
            try (Stream<Path> pathStream = Files.walk(caseDirectory)) {
                Optional<Path> pathOpt = pathStream.filter(file -> !Files.isDirectory(file)).findFirst();
                if (pathOpt.isEmpty()) {
                    throw CaseException.createDirectoryEmpty(caseDirectory);
                }
                return pathOpt.get();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return null;
    }

    Optional<byte[]> getCaseBytes(UUID caseUuid) {
        checkStorageInitialization();

        Path caseFile = getCaseFile(caseUuid);
        if (caseFile == null) {
            return Optional.empty();
        }

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

    boolean caseExists(UUID caseName) {
        checkStorageInitialization();
        Path caseFile = getCaseFile(caseName);
        if (caseFile == null) {
            return false;
        }
        return Files.exists(caseFile) && Files.isRegularFile(caseFile);
    }

    UUID importCase(MultipartFile mpf, boolean toPublic) {
        checkStorageInitialization();

        UUID caseUuid = UUID.randomUUID();
        Path uuidDirectory;
        if (toPublic) {
            uuidDirectory = getPublicStorageDir().resolve(caseUuid.toString());
        } else {
            uuidDirectory = getPrivateStorageDir().resolve(caseUuid.toString());
        }
        String caseName = mpf.getOriginalFilename();
        validateCaseName(caseName);

        if (Files.exists(uuidDirectory)) {
            throw CaseException.createDirectoryAreadyExists(uuidDirectory);
        }

        Path caseFile;
        try {
            Files.createDirectory(uuidDirectory);
            caseFile = uuidDirectory.resolve(caseName);
            mpf.transferTo(caseFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        try {
            getImporterOrThrowsException(caseFile);
        } catch (CaseException e) {
            try {
                Files.deleteIfExists(caseFile);
                Files.deleteIfExists(uuidDirectory);
            } catch (IOException e2) {
                LOGGER.error(e2.toString(), e2);
            }
            throw e;
        }
        CaseInfos caseInfos = new CaseInfos(caseFile.getFileName().toString(), getFormat(caseFile), caseUuid);
        caseInfosPublisher.onNext(CaseInfos.getMessage(caseInfos));
        return caseUuid;
    }

    Optional<Network> loadNetwork(UUID caseUuid) {
        checkStorageInitialization();

        Path caseFile = getCaseFile(caseUuid);
        if (caseFile == null) {
            return Optional.empty();
        }

        if (Files.exists(caseFile) && Files.isRegularFile(caseFile)) {
            Network network = Importers.loadNetwork(caseFile);
            if (network == null) {
                throw CaseException.createFileNotImportable(caseFile);
            }
            return Optional.of(network);
        }
        return Optional.empty();
    }

    void deleteDirectoryRecursively(Path caseDirectory) {
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(caseDirectory)) {
            paths.forEach(file -> {
                try {
                    Files.delete(file);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            Files.delete(caseDirectory);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void deleteCase(UUID caseUuid) {
        checkStorageInitialization();
        Path caseDirectory = getCaseDirectory(caseUuid);
        deleteDirectoryRecursively(caseDirectory);
    }

    void deleteAllCases() {
        checkStorageInitialization();

        List<Path> directories = Arrays.asList(getPrivateStorageDir(), getPublicStorageDir());

        for (Path directory : directories) {
            try (DirectoryStream<Path> paths = Files.newDirectoryStream(directory)) {
                paths.forEach(this::deleteDirectoryRecursively);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public Path getStorageRootDir() {
        return fileSystem.getPath(rootDirectory);
    }

    public Path getPublicStorageDir() {
        return getStorageRootDir().resolve("public");
    }

    public Path getPrivateStorageDir() {
        return getStorageRootDir().resolve("private");
    }

    private boolean isStorageCreated() {
        Path storageRootDir = getStorageRootDir();
        Path publicCases = getPublicStorageDir();
        Path privateCases = getPrivateStorageDir();
        return Files.exists(storageRootDir) && Files.isDirectory(storageRootDir)
                && Files.exists(publicCases) && Files.isDirectory(publicCases)
                && Files.exists(privateCases) && Files.isDirectory(privateCases);

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
