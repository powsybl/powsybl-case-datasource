/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver.datasource.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.jimfs.Jimfs;
import com.powsybl.caseserver.CaseService;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.iidm.import_.Importers;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@RunWith(SpringRunner.class)
@EnableWebMvc
@WebMvcTest(CaseDataSourceController.class)
@TestPropertySource(properties = {"case-store-directory=test"})
public class CaseDataSourceControllerTest {

    private FileSystem fileSystem = Jimfs.newFileSystem();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CaseService caseService;

    @Value("${case-store-directory:#{systemProperties['user.home'].concat(\"/cases\")}}")
    private String rootDirectory;

    private String cgmesName = "CGMES_v2415_MicroGridTestConfiguration_BC_BE_v2.zip";
    private String fileName = "CGMES_v2415_MicroGridTestConfiguration_BC_BE_v2/MicroGridTestConfiguration_BC_BE_DL_V2.xml";

    private static final UUID CASE_UUID = UUID.randomUUID();

    private DataSource dataSource;
    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp() throws URISyntaxException, IOException {
        Path path = fileSystem.getPath(rootDirectory);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        path = fileSystem.getPath(rootDirectory).resolve("public");
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        path = fileSystem.getPath(rootDirectory).resolve("private");
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        Path caseDirectory = fileSystem.getPath(rootDirectory).resolve("private").resolve(CASE_UUID.toString());
        if (!Files.exists(caseDirectory)) {
            Files.createDirectories(caseDirectory);
        }

        caseService.setFileSystem(fileSystem);
        //insert a cgmes in the FS
        try (InputStream cgmesURL = getClass().getResourceAsStream("/" + cgmesName)) {
            Path cgmes = caseDirectory.resolve(cgmesName);
            Files.copy(cgmesURL, cgmes, StandardCopyOption.REPLACE_EXISTING);
        }
        dataSource = Importers.createDataSource(Paths.get(getClass().getResource("/" + cgmesName).toURI()));
    }

    @Test
    public void testBaseName() throws Exception {
        MvcResult mvcResult = mvc.perform(get("/v1/cases/{caseUuid}/datasource/baseName", CASE_UUID))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(dataSource.getBaseName(), mvcResult.getResponse().getContentAsString());
    }

    @Test
    public void testListName() throws Exception {
        MvcResult mvcResult = mvc.perform(get("/v1/cases/{caseUuid}/datasource/list", CASE_UUID)
                .param("regex", ".*"))
                .andExpect(status().isOk())
                .andReturn();

        Set nameList = mapper.readValue(mvcResult.getResponse().getContentAsString(), Set.class);
        assertEquals(dataSource.listNames(".*"), nameList);
    }

    @Test
    public void testInputStreamWithFileName() throws Exception {
        MvcResult mvcResult = mvc.perform(get("/v1/cases/{caseUuid}/datasource", CASE_UUID)
                .param("fileName", fileName))
                .andExpect(status().isOk())
                .andReturn();

        try (InputStreamReader isReader = new InputStreamReader(dataSource.newInputStream(fileName), StandardCharsets.UTF_8)) {
            BufferedReader reader = new BufferedReader(isReader);
            StringBuilder datasourceResponse = new StringBuilder();
            String str;
            while ((str = reader.readLine()) != null) {
                datasourceResponse.append(str).append("\n");
            }
            assertEquals(datasourceResponse.toString(), mvcResult.getResponse().getContentAsString());
        }
    }

    @Test
    public void testInputStreamWithSuffixExt() throws Exception {
        String suffix = "/MicroGridTestConfiguration_BC_BE_DL_V2";
        String ext = "xml";
        MvcResult mvcResult = mvc.perform(get("/v1/cases/{caseUuid}/datasource", CASE_UUID)
                .param("suffix", suffix)
                .param("ext", ext))
                .andExpect(status().isOk())
                .andReturn();

        try (InputStreamReader isReader = new InputStreamReader(dataSource.newInputStream(suffix, ext), StandardCharsets.UTF_8)) {
            BufferedReader reader = new BufferedReader(isReader);
            StringBuilder datasourceResponse = new StringBuilder();
            String str;
            while ((str = reader.readLine()) != null) {
                datasourceResponse.append(str).append("\n");
            }
            assertEquals(datasourceResponse.toString(), mvcResult.getResponse().getContentAsString());
        }
    }

    @Test
    public void testExistsWithFileName() throws Exception {
        MvcResult mvcResult = mvc.perform(get("/v1/cases/{caseUuid}/datasource/exists", CASE_UUID)
                .param("fileName", fileName))
                .andExpect(status().isOk())
                .andReturn();

        Boolean res = mapper.readValue(mvcResult.getResponse().getContentAsString(), Boolean.class);
        assertEquals(dataSource.exists(fileName), res);

        mvcResult = mvc.perform(get("/v1/cases/{caseUuid}/datasource/exists", CASE_UUID)
                .param("fileName", "random"))
                .andExpect(status().isOk())
                .andReturn();

        res = mapper.readValue(mvcResult.getResponse().getContentAsString(), Boolean.class);
        assertEquals(dataSource.exists("random"), res);
    }

    @Test
    public void testExistsWithSuffixExt() throws Exception {
        String suffix = "random";
        String ext = "uct";
        MvcResult mvcResult = mvc.perform(get("/v1/cases/{caseUuid}/datasource/exists", CASE_UUID)
                .param("suffix", suffix)
                .param("ext", ext))
                .andExpect(status().isOk())
                .andReturn();

        Boolean res = mapper.readValue(mvcResult.getResponse().getContentAsString(), Boolean.class);
        assertEquals(dataSource.exists(suffix, ext), res);
    }

}
