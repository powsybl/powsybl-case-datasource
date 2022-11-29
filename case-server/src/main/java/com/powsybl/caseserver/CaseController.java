/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver;

import com.powsybl.caseserver.dto.CaseInfos;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.xml.NetworkXml;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static com.powsybl.caseserver.CaseException.createDirectoryNotFound;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@RestController
@RequestMapping(value = "/" + CaseConstants.API_VERSION)
@Tag(name = "Case server")
@ComponentScan(basePackageClasses = {CaseService.class})
public class CaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseController.class);

    @Autowired
    private CaseService caseService;

    @GetMapping(value = "/cases")
    @Operation(summary = "Get all cases")
    //For maintenance purpose
    public ResponseEntity<List<CaseInfos>> getCases() {
        LOGGER.debug("getCases request received");
        List<CaseInfos> cases = caseService.getCases(caseService.getStorageRootDir());
        if (cases == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok().body(cases);
    }

    @GetMapping(value = "/cases/{caseUuid}/infos")
    @Operation(summary = "Get a case infos")
    public ResponseEntity<CaseInfos> getCaseInfos(@PathVariable("caseUuid") UUID caseUuid) {
        LOGGER.debug("getCaseInfos request received");
        Path file = caseService.getCaseFile(caseUuid);
        if (file == null) {
            return ResponseEntity.noContent().build();
        }
        CaseInfos caseInfos = caseService.getCase(file);
        return ResponseEntity.ok().body(caseInfos);
    }

    @GetMapping(value = "/cases/{caseUuid}/format")
    @Operation(summary = "Get case Format")
    public ResponseEntity<String> getCaseFormat(@PathVariable("caseUuid") UUID caseUuid) {
        LOGGER.debug("getCaseFormat request received");
        Path file = caseService.getCaseFile(caseUuid);
        if (file == null) {
            throw createDirectoryNotFound(caseUuid);
        }
        String caseFormat = caseService.getFormat(file);
        return ResponseEntity.ok().body(caseFormat);
    }

    @GetMapping(value = "/cases/{caseUuid}/name")
    @Operation(summary = "Get case name")
    public ResponseEntity<String> getCaseName(@PathVariable("caseUuid") UUID caseUuid) {
        LOGGER.debug("getCaseName request received");
        String caseName = caseService.getCaseName(caseUuid);
        return ResponseEntity.ok().body(caseName);
    }

    @GetMapping(value = "/cases/{caseUuid}")
    @Operation(summary = "Get a case")
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
    @Operation(summary = "check if the case exists")
    public ResponseEntity<Boolean> exists(@PathVariable("caseUuid") UUID caseUuid) {
        boolean exists = caseService.caseExists(caseUuid);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(exists);

    }

    @PostMapping(value = "/cases", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "import a case")
    @SuppressWarnings("javasecurity:S5145")
    public ResponseEntity<UUID> importCase(@RequestParam("file") MultipartFile file,
                                           @RequestParam(value = "withExpiration", required = false, defaultValue = "false") boolean withExpiration) {
        LOGGER.debug("importCase request received with file = {}", file.getName());
        UUID caseUuid = caseService.importCase(file, withExpiration);
        return ResponseEntity.ok().body(caseUuid);
    }

    @PostMapping(value = "/cases")
    @Operation(summary = "create a case from an existing one")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The case has been duplicated"),
            @ApiResponse(responseCode = "404", description = "Source case not found"),
            @ApiResponse(responseCode = "500", description = "An error occurred during the case file duplication")})
    public ResponseEntity<UUID> createCase(@RequestParam("duplicateFrom") UUID sourceCaseUuid,
                                           @RequestParam(value = "withExpiration", required = false, defaultValue = "false") boolean withExpiration) {
        LOGGER.info("createCase request received with parameter sourceCaseUuid = {}", sourceCaseUuid);
        UUID newCaseUuid = caseService.createCase(sourceCaseUuid, withExpiration);
        return ResponseEntity.ok().body(newCaseUuid);
    }

    @DeleteMapping(value = "/cases/{caseUuid}/expiration")
    @Operation(summary = "disable the case expiration")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The case expiration has been removed"),
            @ApiResponse(responseCode = "404", description = "Source case not found")})
    public ResponseEntity<Void> disableCaseExpiration(@PathVariable("caseUuid") UUID caseUuid) {
        LOGGER.info("disableCaseExpiration request received for caseUuid = {}", caseUuid);
        caseService.disableCaseExpiration(caseUuid);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/cases/{caseUuid}")
    @Operation(summary = "delete a case")
    public ResponseEntity<Void> deleteCase(@PathVariable("caseUuid") UUID caseUuid) {
        LOGGER.debug("deleteCase request received with parameter caseUuid = {}", caseUuid);
        caseService.deleteCase(caseUuid);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/cases")
    @Operation(summary = "delete all cases")
    public ResponseEntity<Void> deleteCases() {
        LOGGER.debug("deleteCases request received");
        caseService.deleteAllCases();
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/cases/search")
    @Operation(summary = "Search cases by metadata")
    public ResponseEntity<List<CaseInfos>> searchCases(@RequestParam(value = "q") String query) {
        LOGGER.debug("search cases request received");
        List<CaseInfos> cases = caseService.searchCases(query);
        return ResponseEntity.ok().body(cases);
    }

    @PostMapping(value = "/cases/reindex-all")
    @Operation(summary = "reindex all cases")
    public ResponseEntity<Void> reindexAllCases() {
        LOGGER.debug("reindex all cases request received");
        caseService.reindexAllCases();
        return ResponseEntity.ok().build();
    }
}
