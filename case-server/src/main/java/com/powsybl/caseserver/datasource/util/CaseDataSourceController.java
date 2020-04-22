/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver.datasource.util;

import com.powsybl.caseserver.CaseConstants;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@RestController
@RequestMapping(value = "/" + CaseConstants.API_VERSION)
@Api(value = "Case datasource util")
@ComponentScan(basePackageClasses = {CaseDataSourceService.class})
public class CaseDataSourceController {

    @Autowired
    private CaseDataSourceService caseDataSourceService;

    @GetMapping(value = "/cases/{caseUuid}/datasource/baseName")
    @ApiOperation(value = "Get datasource baseName")
    public ResponseEntity<String> getBaseName(@PathVariable("caseUuid") UUID caseUuid) {
        String baseName = caseDataSourceService.getBaseName(caseUuid);
        return ResponseEntity.ok().body(baseName);
    }

    @GetMapping(value = "/cases/{caseUuid}/datasource/exists", params = {"suffix", "ext"})
    @ApiOperation(value = "check if the file exists in the datasource")
    public ResponseEntity<Boolean> datasourceExists(@PathVariable("caseUuid") UUID caseUuid,
                                                    @RequestParam String suffix,
                                                    @RequestParam String ext) {
        Boolean exists = caseDataSourceService.datasourceExists(caseUuid, suffix, ext);
        return ResponseEntity.ok().body(exists);
    }

    @GetMapping(value = "/cases/{caseUuid}/datasource/exists", params = "fileName")
    @ApiOperation(value = "check if the file exists in the datasource")
    public ResponseEntity<Boolean> datasourceExists(@PathVariable("caseUuid") UUID caseUuid,
                                                    @RequestParam String fileName) {
        Boolean exists = caseDataSourceService.datasourceExists(caseUuid, fileName);
        return ResponseEntity.ok().body(exists);
    }

    @GetMapping(value = "/cases/{caseUuid}/datasource", params = {"suffix", "ext"})
    @ApiOperation(value = "Get an input stream")
    public ResponseEntity<StreamingResponseBody> getFileData(@PathVariable("caseUuid") UUID caseUuid,
                                                      @RequestParam(value = "suffix") String suffix,
                                                      @RequestParam(value = "ext") String ext) {
        byte[] byteArray = caseDataSourceService.getInputStream(caseUuid, suffix, ext);
        StreamingResponseBody stream = outputStream -> outputStream.write(byteArray);
        return ResponseEntity.ok().contentType(new MediaType("text", "plain", StandardCharsets.UTF_8)).body(stream);
    }

    @GetMapping(value = "/cases/{caseUuid}/datasource", params = "fileName")
    @ApiOperation(value = "Get an input stream")
    public ResponseEntity<StreamingResponseBody> getFileData(@PathVariable("caseUuid") UUID caseUuid,
                                                             @RequestParam(value = "fileName") String fileName) {
        byte[] byteArray = caseDataSourceService.getInputStream(caseUuid, fileName);
        StreamingResponseBody stream = outputStream -> outputStream.write(byteArray);
        return ResponseEntity.ok().contentType(new MediaType("text", "plain", StandardCharsets.UTF_8)).body(stream);
    }

    @GetMapping(value = "/cases/{caseUuid}/datasource/list")
    @ApiOperation(value = "list the files names matching the regex")
    public ResponseEntity<Set<String>> listName(@PathVariable("caseUuid") UUID caseUuid,
                                                @RequestParam(value = "regex") String regex) {
        Set<String> nameList = caseDataSourceService.listName(caseUuid, regex);
        return ResponseEntity.ok().body(nameList);
    }

}
