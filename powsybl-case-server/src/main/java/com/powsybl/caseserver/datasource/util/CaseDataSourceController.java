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

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@RestController
@RequestMapping(value = "/" + CaseConstants.CASE_API_VERSION)
@Api(value = "Case datasource util")
@ComponentScan(basePackageClasses = {CaseDataSourceService.class})
public class CaseDataSourceController {

    @Autowired
    private CaseDataSourceService caseDataSourceService;

    @GetMapping(value = "/cases/{caseName}/datasource/baseName")
    @ApiOperation(value = "Get datasource baseName")
    public ResponseEntity<String> getBaseName(@PathVariable("caseName") String caseName) {
        String baseName = caseDataSourceService.getBaseName(caseName);
        return ResponseEntity.ok().body(baseName);
    }

    @GetMapping(value = "/cases/{caseName}/datasource/exists", params = {"suffix", "ext"})
    @ApiOperation(value = "check if the file exists in the datasource")
    public ResponseEntity<Boolean> datasourceExists(@PathVariable("caseName") String caseName,
                                                    @RequestParam String suffix,
                                                    @RequestParam String ext) {
        Boolean exists = caseDataSourceService.datasourceExists(caseName, suffix, ext);
        return ResponseEntity.ok().body(exists);
    }

    @GetMapping(value = "/cases/{caseName}/datasource/exists", params = "fileName")
    @ApiOperation(value = "check if the file exists in the datasource")
    public ResponseEntity<Boolean> datasourceExists(@PathVariable("caseName") String caseName,
                                                    @RequestParam String fileName) {
        Boolean exists = caseDataSourceService.datasourceExists(caseName, fileName);
        return ResponseEntity.ok().body(exists);
    }

    @GetMapping(value = "/cases/{caseName}/datasource", params = {"suffix", "ext"})
    @ApiOperation(value = "Get an input stream")
    public ResponseEntity<StreamingResponseBody> getFileData(@PathVariable("caseName") String caseName,
                                                      @RequestParam(value = "suffix") String suffix,
                                                      @RequestParam(value = "ext") String ext) {
        byte[] byteArray = caseDataSourceService.getInputStream(caseName, suffix, ext);
        StreamingResponseBody stream = outputStream -> outputStream.write(byteArray);
        return ResponseEntity.ok().contentType(new MediaType("text", "plain", StandardCharsets.UTF_8)).body(stream);
    }

    @GetMapping(value = "/cases/{caseName}/datasource", params = "fileName")
    @ApiOperation(value = "Get an input stream")
    public ResponseEntity<StreamingResponseBody> getFileData(@PathVariable("caseName") String caseName,
                                                             @RequestParam(value = "fileName") String fileName) {
        byte[] byteArray = caseDataSourceService.getInputStream(caseName, fileName);
        StreamingResponseBody stream = outputStream -> outputStream.write(byteArray);
        return ResponseEntity.ok().contentType(new MediaType("text", "plain", StandardCharsets.UTF_8)).body(stream);
    }

    @GetMapping(value = "/cases/{caseName}/datasource/list")
    @ApiOperation(value = "list the files names matching the regex")
    public ResponseEntity<Set<String>> listName(@PathVariable("caseName") String caseName,
                                                @RequestParam(value = "regex") String regex) {
        Set<String> nameList = caseDataSourceService.listName(caseName, regex);
        return ResponseEntity.ok().body(nameList);
    }

}
