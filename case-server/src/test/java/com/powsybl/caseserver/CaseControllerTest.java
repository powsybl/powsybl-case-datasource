/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.computation.ComputationManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@RunWith(SpringRunner.class)
@EnableWebMvc
@WebMvcTest(CaseController.class)
@ContextConfiguration(classes = {CaseController.class, CaseService.class})
@TestPropertySource(properties = {"case-store-directory=/cases"})
public class CaseControllerTest {

    private static final String TEST_CASE = "testCase.xiidm";
    private static final String NOT_A_NETWORK = "notANetwork.txt";
    private static final String STILL_NOT_A_NETWORK = "stillNotANetwork.xiidm";

    private static final String GET_CASE_URL = "/v1/cases/{caseName}";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CaseService caseService;

    @Value("${case-store-directory}")
    private String rootDirectory;

    private FileSystem fileSystem;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        caseService.setFileSystem(fileSystem);
        caseService.setComputationManager(Mockito.mock(ComputationManager.class));
    }

    @After
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    private void createStorageDir() throws IOException {
        Path path = fileSystem.getPath(rootDirectory);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    private static MockMultipartFile createMockMultipartFile(String fileName) throws IOException {
        MockMultipartFile mockFile;
        try (InputStream inputStream = CaseControllerTest.class.getResourceAsStream("/" + fileName)) {
            mockFile = new MockMultipartFile("file", fileName, MediaType.TEXT_PLAIN_VALUE, inputStream);
        }
        return mockFile;
    }

    @Test
    public void test() throws Exception {
        // expect a fail since the storage dir. is not created
        mvc.perform(delete("/v1/cases"))
                .andExpect(status().isUnprocessableEntity());

        // create the storage dir
        createStorageDir();

        // now it must work
        mvc.perform(delete("/v1/cases"))
                .andExpect(status().isOk());

        // check if the case exists (except a false)
        mvc.perform(get("/v1/cases/{caseName}/exists", TEST_CASE))
                .andExpect(status().isOk())
                .andExpect(content().string("false"))
                .andReturn();

        // import a case
        mvc.perform(multipart("/v1/cases")
                .file(createMockMultipartFile(TEST_CASE)))
                .andExpect(status().isOk())
                .andReturn();

        // check if the case exists (except a true)
        mvc.perform(get("/v1/cases/{caseName}/exists", TEST_CASE))
                .andExpect(status().isOk())
                .andExpect(content().string("true"))
                .andReturn();

        // import the same case and expect a fail
        mvc.perform(multipart("/v1/cases")
                .file(createMockMultipartFile(TEST_CASE)))
                .andExpect(status().is(409))
                .andExpect(content().string(startsWith("A file with the same name already exists")))
                .andReturn();

        // import a non valid case and expect a fail
        mvc.perform(multipart("/v1/cases")
                .file(createMockMultipartFile(NOT_A_NETWORK)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(startsWith("This file cannot be imported")))
                .andReturn();

        // import a non valid case with a valid extension and expect a fail
        mvc.perform(multipart("/v1/cases")
                .file(createMockMultipartFile(STILL_NOT_A_NETWORK)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(startsWith("This file cannot be imported")))
                .andReturn();

        // list the cases and expect the case added just before
        mvc.perform(get("/v1/cases"))
                .andExpect(status().isOk())
                .andExpect(content().json("[{name: 'testCase.xiidm', format: 'XIIDM'}]"))
                .andReturn();

        // retrieve a case as a network
        MvcResult mvcResult =  mvc.perform(get(GET_CASE_URL, TEST_CASE)
                .param("xiidm", "false"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        String testCaseContent = new String(ByteStreams.toByteArray(getClass().getResourceAsStream("/" + TEST_CASE)), StandardCharsets.UTF_8);
        mvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().xml(testCaseContent))
                .andReturn();

        // retrieve a case (async)
        mvcResult = mvc.perform(get(GET_CASE_URL, TEST_CASE))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().xml(testCaseContent))
                .andReturn();

        // retrieve a non existing case (async)
        mvc.perform(get(GET_CASE_URL, "non-existing"))
                .andExpect(status().isNoContent())
                .andReturn();

        // delete the case
        mvc.perform(delete(GET_CASE_URL, TEST_CASE))
                .andExpect(status().isOk());

        // delete non existing file
        mvc.perform(delete(GET_CASE_URL, TEST_CASE))
                .andExpect(content().string(startsWith("The file requested doesn't exist")))
                .andReturn();

        // import a case to delete it
        mvc.perform(multipart("/v1/cases")
                .file(createMockMultipartFile(TEST_CASE)))
                .andExpect(status().isOk())
                .andReturn();

        // delete all cases
        mvc.perform(delete("/v1/cases"))
                .andExpect(status().isOk());
    }

    @Test
    public void validateCaseNameTest() {
        CaseService.validateCaseName("test");
        CaseService.validateCaseName("test.xiidm");
        CaseService.validateCaseName("te-st");
        CaseService.validateCaseName("test-case.7zip");
        CaseService.validateCaseName("testcase1.7zip");

        try {
            CaseService.validateCaseName("../test.xiidm");
            fail();
        } catch (CaseException ignored) {
        }
        try {
            CaseService.validateCaseName("test..xiidm");
            fail();
        } catch (CaseException ignored) {
        }
        try {
            CaseService.validateCaseName("test/xiidm");
            fail();
        } catch (CaseException ignored) {
        }

    }
}
