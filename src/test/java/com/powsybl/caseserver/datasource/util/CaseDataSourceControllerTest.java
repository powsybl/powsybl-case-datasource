/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver.datasource.util;

import com.google.common.jimfs.Jimfs;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.powsybl.caseserver.CaseService;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.iidm.import_.Importers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.io.*;
import java.net.URISyntaxException;

import java.nio.file.*;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@RunWith(SpringRunner.class)
@EnableWebMvc
@WebMvcTest(CaseDataSourceController.class)
@ContextConfiguration(classes = {CaseDataSourceController.class, CaseDataSourceService.class})
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
    private String fileName = "CGMES_v2.4.15_MicroGridTestConfiguration_BC_BE_v2/MicroGridTestConfiguration_BC_BE_DL_V2.xml";

    private DataSource dataSource;

    @Before
    public void setUp() throws URISyntaxException, IOException {
        Path path = fileSystem.getPath(rootDirectory);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                fail();
            }
        }
        caseService.setFileSystem(fileSystem);
        //insert a cgmes in the FS
        try (InputStream cgmesURL = getClass().getResourceAsStream("/" + cgmesName)) {
            Path cgmes = path.resolve(cgmesName);
            Files.copy(cgmesURL, cgmes, StandardCopyOption.REPLACE_EXISTING);
        }
        dataSource = Importers.createDataSource(Paths.get(getClass().getResource("/" + cgmesName).toURI()));
    }

    @Test
    public void testBaseName() throws Exception {
        MvcResult mvcResult = mvc.perform(get("/v1/cases/{caseName}/datasource/baseName", cgmesName))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(dataSource.getBaseName(), mvcResult.getResponse().getContentAsString());
    }

    @Test
    public void testListName() throws Exception {
        MvcResult mvcResult = mvc.perform(get("/v1/cases/{caseName}/datasource/list", cgmesName)
                .param("regex", ".*"))
                .andExpect(status().isOk())
                .andReturn();

        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .create();
        Set nameList = gson.fromJson(mvcResult.getResponse().getContentAsString(), Set.class);

        assertEquals(dataSource.listNames(".*"), nameList);
    }

    @Test
    public void testInputStreamWithFileName() throws Exception {
        MvcResult mvcResult = mvc.perform(get("/v1/cases/{caseName}/datasource", cgmesName)
                .param("fileName", fileName))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvcResult = mvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andReturn();

        InputStreamReader isReader = new InputStreamReader(dataSource.newInputStream(fileName));
        BufferedReader reader = new BufferedReader(isReader);
        StringBuilder datasourceResponse = new StringBuilder();
        String str;
        while ((str = reader.readLine()) != null) {
            datasourceResponse.append(str).append("\n");
        }
        assertEquals(datasourceResponse.toString(), mvcResult.getResponse().getContentAsString());
    }

    @Test
    public void testExistsWithFileName() throws Exception {
        MvcResult mvcResult = mvc.perform(get("/v1/cases/{caseName}/datasource/exists", cgmesName)
                .param("fileName", fileName))
                .andExpect(status().isOk())
                .andReturn();

        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .create();

        Boolean res = gson.fromJson(mvcResult.getResponse().getContentAsString(), Boolean.class);
        assertEquals(dataSource.exists(fileName), res);

        mvcResult = mvc.perform(get("/v1/cases/{caseName}/datasource/exists", cgmesName)
                .param("fileName", "random"))
                .andExpect(status().isOk())
                .andReturn();

        res = gson.fromJson(mvcResult.getResponse().getContentAsString(), Boolean.class);
        assertEquals(dataSource.exists("random"), res);
    }

    @Test
    public void testExistsWithSuffixExt() throws Exception {
        String suffix = "random";
        String ext = "uct";
        MvcResult mvcResult = mvc.perform(get("/v1/cases/{caseName}/datasource/exists", cgmesName)
                .param("suffix", suffix)
                .param("ext", ext))
                .andExpect(status().isOk())
                .andReturn();

        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .create();

        Boolean res = gson.fromJson(mvcResult.getResponse().getContentAsString(), Boolean.class);
        assertEquals(dataSource.exists(suffix, ext), res);

    }

}
