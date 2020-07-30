/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver;

import com.powsybl.caseserver.dao.CaseInfosDAO;
import com.powsybl.caseserver.dto.CaseInfos;
import com.powsybl.caseserver.dto.entsoe.EntsoeCaseInfos;
import com.powsybl.caseserver.parsers.FileNameInfos;
import com.powsybl.caseserver.parsers.FileNameParser;
import com.powsybl.caseserver.parsers.FileNameParsers;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.entsoe.util.EntsoeGeographicalCode;
import com.powsybl.iidm.import_.Importer;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;

import static com.powsybl.caseserver.parsers.entsoe.EntsoeFileNameParser.parseDateTime;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@Service
public class CaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseService.class);

    private FileSystem fileSystem = FileSystems.getDefault();

    private ComputationManager computationManager = LocalComputationManager.getDefault();

    private final EmitterProcessor<Message<String>> caseInfosPublisher = EmitterProcessor.create();

    @Autowired
    private CaseInfosDAO caseInfosDAO;

    @Bean
    public Supplier<Flux<Message<String>>> publishCaseImport() {
        return () -> caseInfosPublisher;
    }

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

    List<CaseInfos> getCases() {
        checkStorageInitialization();
        try (Stream<Path> walk = Files.walk(getPublicStorageDir())) {
            return walk.filter(Files::isRegularFile)
                    .map(file -> createInfos(file.getFileName().toString(), UUID.fromString(file.getParent().getFileName().toString()), getFormat(file)))
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
        caseInfosDAO.addCaseInfos(caseInfos);
        caseInfosPublisher.onNext(caseInfos.createMessage());

        return caseUuid;
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
        if (!caseName.matches("^[\\w0-9\\-]+(\\.[\\w0-9]+)*$")) {
            throw CaseException.createIllegalCaseName(caseName);
        }
    }

    List<CaseInfos> searchCases(String query) {
        checkStorageInitialization();
        List<DateTime> dates = new ArrayList<>();
        List<EntsoeGeographicalCode> entsoeCodes = new ArrayList<>();

        String decodedQuery;
        try {
            decodedQuery = URLDecoder.decode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new PowsyblException("Error when decoding the query string");
        }

        // the query is an elasticsearch form query, so here it will be :
        // date:XXX AND tsos:(X)
        // date:XXX AND tsos:(X OR Y OR Z)
        //
        String[] searchFields = decodedQuery.split(" AND ");
        for (String searchField : searchFields) {
            String[] field = searchField.split(":");
            if (field.length > 1) {
                if (field[0].equals("date")) {
                    String date = field[1].trim();
                    if (!StringUtils.isEmpty(date)) {
                        dates.add(parseDateTime(date));
                    }
                } else if (field[0].equals("tsos")) {
                    String tmp = field[1].trim();
                    if (tmp.length() > 1) {
                        String[] tsos = tmp.substring(1, tmp.length() - 1).split(" OR ");
                        for (String tso : tsos) {
                            if (!StringUtils.isEmpty(tso)) {
                                entsoeCodes.add(EntsoeGeographicalCode.valueOf(tso.trim()));
                            }
                        }
                    }
                }
            }
        }

        try (Stream<Path> walk = Files.walk(getPublicStorageDir())) {
            return walk.filter(Files::isRegularFile)
                    .map(file -> createInfos(file.getFileName().toString(), UUID.fromString(file.getParent().getFileName().toString()), getFormat(file)))
                    .filter(caseInfos -> {
                        if (caseInfos instanceof EntsoeCaseInfos) {
                            if (!dates.isEmpty() && !entsoeCodes.isEmpty()) {
                                return dates.contains(((EntsoeCaseInfos) caseInfos).getDate()) &&
                                        entsoeCodes.contains(((EntsoeCaseInfos) caseInfos).getGeographicalCode());
                            } else if (dates.isEmpty() && entsoeCodes.isEmpty()) {
                                return true;
                            } else if (!dates.isEmpty() && entsoeCodes.isEmpty()) {
                                return dates.contains(((EntsoeCaseInfos) caseInfos).getDate());
                            } else if (dates.isEmpty() && !entsoeCodes.isEmpty()) {
                                return entsoeCodes.contains(((EntsoeCaseInfos) caseInfos).getGeographicalCode());
                            }
                        } else if (caseInfos instanceof CaseInfos) {
                            return entsoeCodes.isEmpty() && dates.isEmpty();
                        }
                        return false;
                    }).collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
