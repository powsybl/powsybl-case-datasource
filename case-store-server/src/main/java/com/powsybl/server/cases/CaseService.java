/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.server.cases;

import com.powsybl.server.caze.model.CaseException;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.import_.Importers;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.powsybl.server.cases.CaseConstants.*;

@Service
public class CaseService {

    public Map<String, String> getCaseList() {
        checkStorageInitialization();

        Map<String, String> cases;
        try (Stream<Path> walk = Files.walk(Paths.get(System.getProperty(USERHOME) + CASE_FOLDER))) {
            cases = walk.filter(Files::isRegularFile)
                    .collect(Collectors.toMap(x -> x.toString(), x -> x.getFileName().toString()));
        } catch (IOException e) {
            return null;
        }
        return cases;
    }

    public File getCase(String caseName) {
        checkStorageInitialization();

        File file = new File(System.getProperty(USERHOME) + CASE_FOLDER + caseName);
        if (file.exists() && file.isFile()) {
            return file;
        }
        return null;

    }

    public Boolean exists(String caseName) {
        File file = new File(System.getProperty(USERHOME) + CASE_FOLDER + caseName);
        return file.isFile() && file.exists();
    }

    public String importCase(MultipartFile mpf) {
        checkStorageInitialization();

        File file = new File(System.getProperty(USERHOME) + CASE_FOLDER + mpf.getOriginalFilename());
        if (file.exists() && file.isFile()) {
            return FILE_ALREADY_EXISTS;
        }
        try {
            mpf.transferTo(file);
        } catch (IOException e) {
            return IMPORT_FAIL;
        }
        return DONE;
    }

    public Network downloadNetwork(String caseName) {
        checkStorageInitialization();
        Path caseFile = Paths.get(System.getProperty(USERHOME) + CASE_FOLDER + caseName);
        Network network = Importers.loadNetwork(caseFile);
        if (network == null) {
            throw new CaseException("File '" + caseFile + "' is not importable");
        }
        return network;
    }

    public String deleteCase(String caseName) {
        checkStorageInitialization();

        Path path = Paths.get(System.getProperty(USERHOME) + CASE_FOLDER + caseName);
        if (path.toFile().exists() && !path.toFile().isFile()) {
            return FILE_DOESNT_EXIST;
        }
        try {
            Files.delete(path);
            return DONE;
        } catch (NoSuchFileException e) {
            return FILE_DOESNT_EXIST;
        } catch (IOException e) {
            return IOEXCEPTION_MESSAGE;
        }
    }

    public String deleteAllCases() {
        checkStorageInitialization();

        File caseFolder = Paths.get(System.getProperty(USERHOME) + CASE_FOLDER).toFile();
        try {
            FileUtils.cleanDirectory(caseFolder);
        } catch (IOException e) {
            return IOEXCEPTION_MESSAGE;
        } catch (IllegalArgumentException e) {
            return DIRECTORY_DOESNT_EXIST;
        }
        return DONE;
    }

    private boolean isStorageCreated() {
        Path storageRooDir = Paths.get(System.getProperty(USERHOME) + CASE_FOLDER);
        return storageRooDir.toFile().exists() && storageRooDir.toFile().isDirectory();
    }

    private void checkStorageInitialization() throws CaseException {
        if (!isStorageCreated()) {
            throw new CaseException(STORAGE_DIR_NOT_CREATED);
        }
    }
}
