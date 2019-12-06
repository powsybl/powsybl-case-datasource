/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.server.cases;

import com.powsybl.server.caze.model.CaseException;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.xml.NetworkXml;
import com.powsybl.server.caze.model.CaseApi;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.util.Map;

import static com.powsybl.server.cases.CaseConstants.*;

@RestController
@RequestMapping(value = "/" + CaseApi.VERSION + "/case-server")
@Api(value = "Case server")
@ComponentScan(basePackageClasses = {CaseService.class})
public class CaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseController.class);

    @Inject
    private CaseService caseService;

    @GetMapping(value = "/cases")
    @ApiOperation(value = "Get all cases")
    public ResponseEntity<Map<String, String>> getCaseList() {
        LOGGER.debug("Cases request received");

        Map<String, String> cases;
        try {
            cases = caseService.getCaseList();
        } catch (CaseException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, STORAGE_DIR_NOT_CREATED);
        }
        if (cases == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, IOEXCEPTION_MESSAGE);
        }
        return ResponseEntity.ok().body(cases);
    }

    @GetMapping(value = "/cases/{caseName:.+}")
    @ApiOperation(value = "Get a case")
    public ResponseEntity<StreamingResponseBody> getCase(@PathVariable("caseName") String caseName) {
        LOGGER.debug("getCase request received with parameter caseName = {}", caseName);

        File file;
        try {
            file = caseService.getCase(caseName);
        } catch (CaseException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, STORAGE_DIR_NOT_CREATED);
        }
        if (file == null) {
            return ResponseEntity.noContent().build();
        }
        try {
            byte[] arrayBytes = Files.readAllBytes(file.toPath());
            StreamingResponseBody stream = outputStream -> outputStream.write(arrayBytes);
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(stream);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, GET_FILE_FAIL);
        }
    }

    @GetMapping(value = "/exists")
    @ApiOperation(value = "check if the case exists")
    public ResponseEntity<Boolean> exists(@RequestParam("caseName") String caseName) {
        Boolean exists = caseService.exists(caseName);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(exists);

    }

    @PostMapping(value = "/import-case")
    @ApiOperation(value = "import a case")
    public ResponseEntity<String> importCase(@RequestParam("file") MultipartFile file) {
        LOGGER.debug("importCase request received with file = {}", file.getName());

        String status;
        try {
            status = caseService.importCase(file);
        } catch (CaseException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, STORAGE_DIR_NOT_CREATED);
        }
        if (status.equals(FILE_ALREADY_EXISTS)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, FILE_ALREADY_EXISTS);
        }
        if (status.equals(IMPORT_FAIL)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, IMPORT_FAIL);
        }
        return ResponseEntity.ok().body(DONE);
    }

    @GetMapping(value = "/download-network")
    @ApiOperation(value = "download a zipped case in xml format")
    public ResponseEntity<byte[]> downloadNetwork(@RequestParam("caseName") String caseName) {
        LOGGER.debug("downloadNetwork request received with parameter caseName = {}", caseName);

        Network network;
        try {
            network = caseService.downloadNetwork(caseName);
        } catch (CaseException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, FILE_NOT_IMPORTABLE);
        } catch (InvalidPathException e) {
            throw new ResponseStatusException(HttpStatus.NO_CONTENT, FILE_DOESNT_EXIST);
        }
        byte[] zipNetwork = NetworkXml.gzip(network);
        return ResponseEntity.ok().body(zipNetwork);
    }

    @DeleteMapping(value = "/cases/{caseName:.+}")
    @ApiOperation(value = "delete a case")
    public ResponseEntity<String> deleteCase(@PathVariable("caseName") String caseName) {
        LOGGER.debug("deleteCase request received with parameter caseName = {}", caseName);

        String status;
        try {
            status = caseService.deleteCase(caseName);
        } catch (CaseException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, STORAGE_DIR_NOT_CREATED);
        }
        if (status.equals(FILE_DOESNT_EXIST)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, FILE_DOESNT_EXIST);
        }
        if (status.equals(IOEXCEPTION_MESSAGE)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, IOEXCEPTION_MESSAGE);
        }
        return ResponseEntity.ok().body(DONE);
    }

    @DeleteMapping(value = "/cases")
    @ApiOperation(value = "delete all cases")
    public ResponseEntity<String> deleteCases() {
        LOGGER.debug("deleteCases request received");

        String status;
        try {
            status = caseService.deleteAllCases();
        } catch (CaseException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, STORAGE_DIR_NOT_CREATED);
        }
        if (status.equals(IOEXCEPTION_MESSAGE)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, IOEXCEPTION_MESSAGE);
        }
        if (status.equals(DIRECTORY_DOESNT_EXIST)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, DIRECTORY_DOESNT_EXIST);
        }
        return ResponseEntity.ok().body(DONE);
    }

}
