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
import com.powsybl.caseserver.repository.CaseMetadataEntity;
import com.powsybl.caseserver.repository.CaseMetadataRepository;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Importer;
import com.powsybl.iidm.network.Importers;
import com.powsybl.iidm.network.Network;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static com.powsybl.caseserver.CaseException.createDirectoryNotFound;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@Service
@ComponentScan(basePackageClasses = {CaseInfosServiceImpl.class, CaseMetadataRepository.class})
public class CaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseService.class);

    private static final String CATEGORY_BROKER_OUTPUT = CaseService.class.getName() + ".output-broker-messages";

    private static final Logger OUTPUT_MESSAGE_LOGGER = LoggerFactory.getLogger(CATEGORY_BROKER_OUTPUT);

    private FileSystem fileSystem = FileSystems.getDefault();

    private ComputationManager computationManager = LocalComputationManager.getDefault();

    private final CaseMetadataRepository caseMetadataRepository;

    @Autowired
    private StreamBridge caseInfosPublisher;

    @Autowired
    @Lazy
    private CaseInfosService caseInfosService;

    @Value("${case-store-directory:#{systemProperties['user.home'].concat(\"/cases\")}}")
    private String rootDirectory;

    public CaseService(CaseMetadataRepository caseMetadataRepository) {
        this.caseMetadataRepository = caseMetadataRepository;
    }

    Importer getImporterOrThrowsException(Path caseFile) {
        DataSource dataSource = Importers.createDataSource(caseFile);
        Importer importer = Importer.find(dataSource, computationManager);
        if (importer == null) {
            throw CaseException.createFileNotImportable(caseFile);
        }
        return importer;
    }

    String getFormat(Path caseFile) {
        Importer importer = getImporterOrThrowsException(caseFile);
        return importer.getFormat();
    }

    public List<CaseInfos> getCases(Path directory) {
        try (Stream<Path> walk = Files.walk(directory)) {
            return walk.filter(Files::isRegularFile)
                .map(file -> createInfos(file.getFileName().toString(), UUID.fromString(file.getParent().getFileName().toString()), getFormat(file)))
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String getCaseName(UUID caseUuid) {
        Path file = getCaseFile(caseUuid);
        if (file == null) {
            throw createDirectoryNotFound(caseUuid);
        }
        CaseInfos caseInfos = getCase(file);
        return caseInfos.getName();
    }

    public CaseInfos getCase(Path casePath) {
        checkStorageInitialization();
        Optional<CaseInfos> caseInfo = getCases(casePath).stream().findFirst();
        return caseInfo.orElseThrow();
    }

    public Path getCaseFile(UUID caseUuid) {
        return walkCaseDirectory(getStorageRootDir().resolve(caseUuid.toString()));
    }

    Path getCaseDirectory(UUID caseUuid) {
        Path caseDirectory = getStorageRootDir().resolve(caseUuid.toString());
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

    UUID importCase(MultipartFile mpf, boolean withExpiration) {
        checkStorageInitialization();

        UUID caseUuid = UUID.randomUUID();
        Path uuidDirectory = getStorageRootDir().resolve(caseUuid.toString());

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

        LocalDateTime expirationTime = null;
        if (withExpiration) {
            expirationTime = LocalDateTime.now(ZoneOffset.UTC).plusHours(1);
        }
        CaseMetadataEntity caseEntity = new CaseMetadataEntity(caseUuid, expirationTime);
        caseMetadataRepository.save(caseEntity);
        CaseInfos caseInfos = createInfos(caseFile.getFileName().toString(), caseUuid, importer.getFormat());
        caseInfosService.addCaseInfos(caseInfos);
        sendImportMessage(caseInfos.createMessage());
        return caseUuid;
    }

    UUID createCase(UUID sourceCaseUuid, boolean withExpiration) {
        try {
            Path existingCaseFile = getCaseFile(sourceCaseUuid);
            if (existingCaseFile == null || existingCaseFile.getParent() == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Source case " + sourceCaseUuid + " not found");
            }

            UUID newCaseUuid = UUID.randomUUID();
            Path newCaseUuidDirectory = existingCaseFile.getParent().getParent().resolve(newCaseUuid.toString());
            Path newCaseFile;
            Files.createDirectory(newCaseUuidDirectory);
            newCaseFile = newCaseUuidDirectory.resolve(existingCaseFile.getFileName());
            Files.copy(existingCaseFile, newCaseFile, StandardCopyOption.COPY_ATTRIBUTES);

            CaseInfos existingCaseInfos = caseInfosService.getCaseInfosByUuid(sourceCaseUuid.toString()).orElseThrow();
            CaseInfos caseInfos = createInfos(existingCaseInfos.getName(), newCaseUuid, existingCaseInfos.getFormat());
            caseInfosService.addCaseInfos(caseInfos);

            LocalDateTime expirationTime = null;
            if (withExpiration) {
                expirationTime = LocalDateTime.now(ZoneOffset.UTC).plusHours(1);
            }
            CaseMetadataEntity caseEntity = new CaseMetadataEntity(newCaseUuid, expirationTime);
            caseMetadataRepository.save(caseEntity);

            sendImportMessage(caseInfos.createMessage());
            return newCaseUuid;

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred during case duplication");
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

    @Transactional
    public void disableCaseExpiration(UUID caseUuid) {
        CaseMetadataEntity caseMetadataEntity = caseMetadataRepository.findById(caseUuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "case " + caseUuid + " not found"));
        caseMetadataEntity.setExpirationDate(null);
    }

    Optional<Network> loadNetwork(UUID caseUuid) {
        checkStorageInitialization();

        Path caseFile = getCaseFile(caseUuid);
        if (caseFile == null) {
            return Optional.empty();
        }

        if (Files.exists(caseFile) && Files.isRegularFile(caseFile)) {
            Network network = Network.read(caseFile);
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
        caseMetadataRepository.deleteById(caseUuid);
    }

    void deleteAllCases() {
        checkStorageInitialization();

        Path rootDirectoryPath = getStorageRootDir();
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(rootDirectoryPath)) {
            paths.forEach(this::deleteDirectoryRecursively);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        caseInfosService.deleteAllCaseInfos();
        caseMetadataRepository.deleteAll();
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
        caseInfosService.recreateAllCaseInfos(getCases(getStorageRootDir()));
    }
}
