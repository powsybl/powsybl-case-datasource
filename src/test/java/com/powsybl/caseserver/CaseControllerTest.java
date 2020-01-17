/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.jimfs.Jimfs;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.xml.NetworkXml;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static com.powsybl.caseserver.CaseConstants.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@RunWith(SpringRunner.class)
@EnableWebMvc
@WebMvcTest(CaseController.class)
@ContextConfiguration(classes = {CaseController.class, CaseService.class})
@TestPropertySource(properties = {"case-store-directory=test"})
public class CaseControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CaseService caseService;

    private FileSystem fileSystem = Jimfs.newFileSystem();

    private static final String TEST_CASE = "testCase.xiidm";
    private static final String NOT_A_NETWORK = "notANetwork.txt";
    private static final String STILL_NOT_A_NETWORK = "stillNotANetwork.xiidm";

    private static final String GET_CASE_URL = "/v1/cases/{caseName}";

    @Value("${case-store-directory:#{systemProperties['user.home'].concat(\"/cases\")}}")
    private String rootDirectory;

    public void setUp() {
        Path path = fileSystem.getPath(rootDirectory);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                fail();
            }
        }
        caseService.setFileSystem(fileSystem);
    }

    @Test
    public void test() throws Exception {
        //expect a fail since the storage dir. is not created
        mvc.perform(delete("/v1/cases"))
                .andExpect(status().isUnprocessableEntity());
        //create the storage dir
        setUp();

        //now it must work
        mvc.perform(delete("/v1/cases"))
                .andExpect(status().isOk());

        //check if the case exists (except a false)
        MvcResult mvcResult = mvc.perform(get("/v1/cases/{caseName}/exists", "testCase.xiidm"))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals("false", mvcResult.getResponse().getContentAsString());

        //Import a case
        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(TEST_CASE)) {
            MockMultipartFile mockFile = new MockMultipartFile("file", TEST_CASE, "text/plain", inputStream);
            mvc.perform(MockMvcRequestBuilders.multipart("/v1/cases")
                    .file(mockFile))
                    .andExpect(status().isOk())
                    .andReturn();
        }

        //check if the case exists (except a true)
        mvcResult = mvc.perform(get("/v1/cases/{caseName}/exists", "testCase.xiidm"))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals("true", mvcResult.getResponse().getContentAsString());

        //Import the same case and expect a fail
        try (InputStream inputStream = classLoader.getResourceAsStream(TEST_CASE)) {
            MockMultipartFile mockFile = new MockMultipartFile("file", TEST_CASE, "text/plain", inputStream);
            mvcResult = mvc.perform(MockMvcRequestBuilders.multipart("/v1/cases")
                    .file(mockFile))
                    .andExpect(status().is(409))
                    .andReturn();
            assertEquals(FILE_ALREADY_EXISTS, mvcResult.getResponse().getContentAsString());
        }

        //Import a non valid case and expect a fail
        try (InputStream inputStream = classLoader.getResourceAsStream(NOT_A_NETWORK)) {
            MockMultipartFile mockFile = new MockMultipartFile("file", NOT_A_NETWORK, "text/plain", inputStream);
            mvcResult = mvc.perform(MockMvcRequestBuilders.multipart("/v1/cases")
                    .file(mockFile))
                    .andExpect(status().isUnprocessableEntity())
                    .andReturn();
            assertEquals(FILE_NOT_IMPORTABLE, mvcResult.getResponse().getContentAsString());
        }

        //Import a non valid case with a valid extension and expect a fail
        try (InputStream inputStream = classLoader.getResourceAsStream(STILL_NOT_A_NETWORK)) {
            MockMultipartFile mockFile = new MockMultipartFile("file", STILL_NOT_A_NETWORK, "text/plain", inputStream);
            mvcResult = mvc.perform(MockMvcRequestBuilders.multipart("/v1/cases")
                    .file(mockFile))
                    .andExpect(status().isUnprocessableEntity())
                    .andReturn();
            assertEquals(FILE_NOT_IMPORTABLE, mvcResult.getResponse().getContentAsString());
        }

        //List the cases and expect the case added just before
        mvcResult = mvc.perform(get("/v1/cases"))
                .andExpect(status().isOk())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        String tmp = mvcResult.getResponse().getContentAsString();
        Map<String, String> listCase = objectMapper.readValue(tmp, Map.class);
        assertTrue(listCase.containsValue(TEST_CASE));

        //Retrieve a case as a network
        mvcResult =  mvc.perform(get(GET_CASE_URL, TEST_CASE)
                .param("xiidm", "false"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        mvcResult = mvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andReturn();

        Network network = NetworkXml.gunzip(mvcResult.getResponse().getContentAsByteArray());
        assertEquals("20140116_0830_2D4_UX1_pst", network.getName());

        //Retrieve a case (async)
        mvcResult = mvc.perform(get(GET_CASE_URL, TEST_CASE))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvcResult = mvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andReturn();

        String xmlNetwork = mvcResult.getResponse().getContentAsString();

        if (xmlNetwork.length() > 20) {
            assertEquals("<?xml version=\"1.0\"", xmlNetwork.substring(0, 19));
        } else {
            fail();
        }

        //Retrieve a non existing case (async)
        mvc.perform(get(GET_CASE_URL, "non-existing"))
                .andExpect(status().isNoContent())
                .andReturn();

        //Delete the case
        mvc.perform(delete(GET_CASE_URL, TEST_CASE))
                .andExpect(status().isOk());

        //Delete non existing file
        mvcResult = mvc.perform(delete(GET_CASE_URL, TEST_CASE))
                .andReturn();

        assertEquals(FILE_DOESNT_EXIST, mvcResult.getResponse().getContentAsString());

        //import a case to delete it
        try (InputStream inputStream = classLoader.getResourceAsStream(TEST_CASE)) {
            MockMultipartFile mockFile = new MockMultipartFile("file", TEST_CASE, "text/plain", inputStream);
            mvc.perform(MockMvcRequestBuilders.multipart("/v1/cases")
                    .file(mockFile))
                    .andExpect(status().isOk())
                    .andReturn();
        }

        //delete all cases
        mvc.perform(delete("/v1/cases"))
                .andExpect(status().isOk());
    }
}
