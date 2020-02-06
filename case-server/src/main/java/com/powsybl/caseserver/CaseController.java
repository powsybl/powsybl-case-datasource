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
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;

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

    @GetMapping(value = "/cases/{caseName:.+}/format")
    @ApiOperation(value = "Get case Format")
    public ResponseEntity<String> getCaseFormat(@PathVariable("caseName") String caseName) {
        LOGGER.debug("getCaseFormat request received");
        Path file = caseService.getCaseFile(caseName);
        String caseFormat = caseService.getFormat(file);
        return ResponseEntity.ok().body(caseFormat);
    }

    @GetMapping(value = "/cases/{caseName:.+}")
    @ApiOperation(value = "Get a case")
    public ResponseEntity<StreamingResponseBody> getCase(@PathVariable("caseName") String caseName,
                                                         @RequestParam(value = "xiidm", required = false, defaultValue = "true") boolean xiidmFormat) {
        LOGGER.debug("getCase request received with parameter caseName = {}", caseName);
        if (xiidmFormat) {
            Network network = caseService.loadNetwork(caseName).orElse(null);
            if (network == null) {
                return ResponseEntity.noContent().build();
            }
            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                NetworkXml.write(network, os);
                os.flush();
                StreamingResponseBody stream = outputStream -> outputStream.write(os.toByteArray());
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_XML)
                        .body(stream);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            byte[] bytes = caseService.getCaseBytes(caseName).orElse(null);
            if (bytes == null) {
                return ResponseEntity.noContent().build();
            }
            StreamingResponseBody stream = outputStream -> outputStream.write(bytes);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(stream);
        }
    }

    @GetMapping(value = "/cases/{caseName:.+}/exists")
    @ApiOperation(value = "check if the case exists")
    public ResponseEntity<Boolean> exists(@PathVariable("caseName") String caseName) {
        boolean exists = caseService.caseExists(caseName);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(exists);

    }

    @PostMapping(value = "/cases")
    @ApiOperation(value = "import a case")
    public ResponseEntity<Void> importCase(@RequestParam("file") MultipartFile file) {
        LOGGER.debug("importCase request received with file = {}", file.getName());
        caseService.importCase(file);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/cases/{caseName:.+}")
    @ApiOperation(value = "delete a case")
    public ResponseEntity<Void> deleteCase(@PathVariable("caseName") String caseName) {
        LOGGER.debug("deleteCase request received with parameter caseName = {}", caseName);
        caseService.deleteCase(caseName);
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
