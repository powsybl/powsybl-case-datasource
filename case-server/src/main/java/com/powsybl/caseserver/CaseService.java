/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver;

import com.powsybl.caseserver.dto.CaseInfos;
import com.powsybl.caseserver.elasticsearch.CaseInfosService;
import com.powsybl.caseserver.elasticsearch.CaseInfosServiceImpl;
import com.powsybl.caseserver.parsers.FileNameInfos;
import com.powsybl.caseserver.parsers.FileNameParser;
import com.powsybl.caseserver.parsers.FileNameParsers;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.Importer;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@Service
@ComponentScan(basePackageClasses = {CaseInfosServiceImpl.class})
public class CaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseService.class);

    private static final String CATEGORY_BROKER_OUTPUT = CaseService.class.getName() + ".output-broker-messages";

    private static final Logger OUTPUT_MESSAGE_LOGGER = LoggerFactory.getLogger(CATEGORY_BROKER_OUTPUT);

    private FileSystem fileSystem = FileSystems.getDefault();

    private ComputationManager computationManager = LocalComputationManager.getDefault();

    @Autowired
    private StreamBridge caseInfosPublisher;

    @Autowired
    @Lazy
    private CaseInfosService caseInfosService;

    @Value("${max-public-cases:-1}")
    Integer maxPublicCases;

    @Value("${case-store-directory:#{systemProperties['user.home'].concat(\"/cases\")}}")
    private String rootDirectory;

    Importer getImporterOrThrowsException(Path caseFile) {
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

    private List<CaseInfos> getCasesFromDirectoryPath(Path directoryPath) {
        try (Stream<Path> walk = Files.walk(directoryPath)) {
            return walk.filter(Files::isRegularFile)
                .map(file -> createInfos(file.getFileName().toString(), UUID.fromString(file.getParent().getFileName().toString()), getFormat(file)))
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<CaseInfos> getCases(boolean onlyPublicCases) {
        checkStorageInitialization();
        List<CaseInfos> casesInfos = getCasesFromDirectoryPath(getPublicStorageDir());
        if (!onlyPublicCases) {
            casesInfos.addAll(getCasesFromDirectoryPath(getPrivateStorageDir()));
        }
        return casesInfos;
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
            ensureMaxCount(getPublicStorageDir().normalize(), maxPublicCases);
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

        Importer importer;
        try {
            importer = getImporterOrThrowsException(caseFile);
        } catch (CaseException e) {
            try {
                Files.deleteIfExists(caseFile);
                Files.deleteIfExists(uuidDirectory);
            } catch (IOException e2) {
                LOGGER.error(e2.toString(), e2);
            }
            throw e;
        }

        CaseInfos caseInfos = createInfos(caseFile.getFileName().toString(), caseUuid, importer.getFormat());
        caseInfosService.addCaseInfos(caseInfos);
        sendImportMessage(caseInfos.createMessage());
        return caseUuid;
    }

    UUID createCase(UUID parentCaseUuid) {
        try {
            Path existingCaseFile = getCaseFile(parentCaseUuid);
            UUID newCaseUuid = UUID.randomUUID();
            Path newCaseUuidDirectory = existingCaseFile.getParent().getParent().resolve(newCaseUuid.toString());

            if (Files.exists(newCaseUuidDirectory)) {
                throw CaseException.createDirectoryAreadyExists(newCaseUuidDirectory);
            }

            Path newCaseFile;
            try {
                Files.createDirectory(newCaseUuidDirectory);
                newCaseFile = newCaseUuidDirectory.resolve(existingCaseFile.getFileName());
                Files.copy(existingCaseFile, newCaseFile, StandardCopyOption.COPY_ATTRIBUTES);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            Optional<CaseInfos> existingCaseInfos = caseInfosService.getCaseInfosByUuid(parentCaseUuid.toString());
            CaseInfos caseInfos = createInfos(existingCaseInfos.get().getName(), newCaseUuid, existingCaseInfos.get().getFormat());
            caseInfosService.addCaseInfos(caseInfos);
            sendImportMessage(caseInfos.createMessage());
            return newCaseUuid;
        } catch (NoSuchElementException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent case " + parentCaseUuid + " not found");
        }
    }

    private void ensureMaxCount(Path directory, int capacity) {
        if (capacity < 0) {
            return;
        }
        List<Path> listFiles = new ArrayList<>();
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(directory)) {
            paths.forEach(listFiles::add);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
        if (listFiles.size() >= capacity) {

            listFiles.sort(Comparator.comparingLong(o -> {
                try {
                    return Files.getLastModifiedTime(o).toMillis();
                } catch (IOException e) {
                    LOGGER.error(e.getMessage());
                    return 0;
                }
            }));
            for (Path path : listFiles.subList(0, listFiles.size() - capacity + 1)) {
                deleteDirectoryRecursively(path);
                caseInfosService.deleteCaseInfosByUuid(path.getFileName().toString());
            }
        }
    }

    CaseInfos createInfos(String fileBaseName, UUID caseUuid, String format) {
        FileNameParser parser = FileNameParsers.findParser(fileBaseName);
        if (parser != null) {
            Optional<? extends FileNameInfos> fileNameInfos = parser.parse(fileBaseName);
            if (fileNameInfos.isPresent()) {
                return CaseInfos.create(fileBaseName, caseUuid, format, fileNameInfos.get());
            }
        }
        return CaseInfos.builder().name(fileBaseName).uuid(caseUuid).format(format).build();
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

        caseInfosService.deleteCaseInfosByUuid(caseUuid.toString());
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

        caseInfosService.deleteAllCaseInfos();
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
        return Files.exists(storageRootDir) && Files.isDirectory(storageRootDir);
    }

    public void checkStorageInitialization() {
        if (!isStorageCreated()) {
            throw CaseException.createStorageNotInitialized(getStorageRootDir());
        }
        try {
            Files.createDirectories(getPublicStorageDir());
            Files.createDirectories(getPrivateStorageDir());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
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
        if (!caseName.matches("[\\w\\-]+(\\.[\\w]+)*+")) {
            throw CaseException.createIllegalCaseName(caseName);
        }
    }

    /*
     The query is an elasticsearch (Lucene) form query, so here it will be :
     date:XXX AND geographicalCode:(X)
     date:XXX AND geographicalCode:(X OR Y OR Z)
    */
    List<CaseInfos> searchCases(String query) {
        checkStorageInitialization();

        return caseInfosService.searchCaseInfos(query);
    }

    private void sendImportMessage(Message<String> message) {
        OUTPUT_MESSAGE_LOGGER.debug("Sending message : {}", message);
        caseInfosPublisher.send("publishCaseImport-out-0", message);
    }

    public void reindexAllCases() {
        caseInfosService.recreateAllCaseInfos(getCases(false));
    }
}
