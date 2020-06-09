/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.xml.NetworkXml;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static com.powsybl.caseserver.CaseException.createDirectoryNotFound;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@RestController
@RequestMapping(value = "/" + CaseConstants.API_VERSION)
@Api(value = "Case server")
@ComponentScan(basePackageClasses = {CaseService.class})
public class CaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseController.class);

    @Autowired
    private CaseService caseService;

    @GetMapping(value = "/cases")
    @ApiOperation(value = "Get all cases")
    public ResponseEntity<List<CaseInfos>> getCases() {
        LOGGER.debug("getCases request received");
        List<CaseInfos> cases = caseService.getCases();
        if (cases == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok().body(cases);
    }

    @GetMapping(value = "/cases/{caseUuid}/format")
    @ApiOperation(value = "Get case Format")
    public ResponseEntity<String> getCaseFormat(@PathVariable("caseUuid") UUID caseUuid) {
        LOGGER.debug("getCaseFormat request received");
        Path file = caseService.getCaseFile(caseUuid);
        if (file == null) {
            throw createDirectoryNotFound(caseUuid);
        }
        String caseFormat = caseService.getFormat(file);
        return ResponseEntity.ok().body(caseFormat);
    }

    @GetMapping(value = "/cases/{caseUuid}")
    @ApiOperation(value = "Get a case")
    public ResponseEntity<byte[]> getCase(@PathVariable("caseUuid") UUID caseUuid,
                                                         @RequestParam(value = "xiidm", required = false, defaultValue = "true") boolean xiidmFormat) {
        LOGGER.debug("getCase request received with parameter caseUuid = {}", caseUuid);
        if (xiidmFormat) {
            Network network = caseService.loadNetwork(caseUuid).orElse(null);
            if (network == null) {
                return ResponseEntity.noContent().build();
            }
            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                NetworkXml.write(network, os);
                os.flush();
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_XML)
                        .body(os.toByteArray());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            byte[] bytes = caseService.getCaseBytes(caseUuid).orElse(null);
            if (bytes == null) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(bytes);
        }
    }

    @GetMapping(value = "/cases/{caseUuid}/exists")
    @ApiOperation(value = "check if the case exists")
    public ResponseEntity<Boolean> exists(@PathVariable("caseUuid") UUID caseUuid) {
        boolean exists = caseService.caseExists(caseUuid);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(exists);

    }

    @PostMapping(value = "/cases/private")
    @ApiOperation(value = "import a case in the private directory")
    public ResponseEntity<UUID> importPrivateCase(@RequestParam("file") MultipartFile file) {
        LOGGER.debug("importPrivateCase request received with file = {}", file.getName());
        UUID caseUuid = caseService.importCase(file, false);
        return ResponseEntity.ok().body(caseUuid);
    }

    @PostMapping(value = "/cases/public")
    @ApiOperation(value = "import a case in the public directory")
    public ResponseEntity<UUID> importPublicCase(@RequestParam("file") MultipartFile file) {
        LOGGER.debug("importPublicCase request received with file = {}", file.getName());
        UUID caseUuid = caseService.importCase(file, true);
        return ResponseEntity.ok().body(caseUuid);
    }

    @DeleteMapping(value = "/cases/{caseUuid}")
    @ApiOperation(value = "delete a case")
    public ResponseEntity<Void> deleteCase(@PathVariable("caseUuid") UUID caseUuid) {
        LOGGER.debug("deleteCase request received with parameter caseUuid = {}", caseUuid);
        caseService.deleteCase(caseUuid);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/cases")
    @ApiOperation(value = "delete all cases")
    public ResponseEntity<Void> deleteCases() {
        LOGGER.debug("deleteCases request received");
        caseService.deleteAllCases();
        return ResponseEntity.ok().build();
    }
}
