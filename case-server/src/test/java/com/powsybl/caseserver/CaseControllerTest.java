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
import java.util.UUID;

import static org.hamcrest.Matchers.startsWith;
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
@TestPropertySource(properties = {"case-store-directory=/cases"})
public class CaseControllerTest {

    private static final String TEST_CASE = "testCase.xiidm";
    private static final String NOT_A_NETWORK = "notANetwork.txt";
    private static final String STILL_NOT_A_NETWORK = "stillNotANetwork.xiidm";
    private static final String TEST_CASE_URL = "/v1/cases/ae17bc33-77e2-4aca-b890-0b8cecd15175";

    private static final String GET_CASE_URL = "/v1/cases/{caseUuid}";
    private static final String GET_CASE_FORMAT_URL = "/v1/cases/{caseName}/format";

    private static final UUID RANDOM_UUID = UUID.fromString("3e2b6777-fea5-4e76-9b6b-b68f151373ab");

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
        path = fileSystem.getPath(rootDirectory).resolve("public");
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        path = fileSystem.getPath(rootDirectory).resolve("private");
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }

    }

    private static MockMultipartFile createMockMultipartFile(String fileName) throws IOException {
        try (InputStream inputStream = CaseControllerTest.class.getResourceAsStream("/" + fileName)) {
            return new MockMultipartFile("file", fileName, MediaType.TEXT_PLAIN_VALUE, inputStream);
        }
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
        mvc.perform(get("/v1/cases/{caseUuid}/exists", RANDOM_UUID))
                .andExpect(status().isOk())
                .andExpect(content().string("false"))
                .andReturn();

        // import a case
        String firstCase = mvc.perform(multipart("/v1/cases/private")
                .file(createMockMultipartFile(TEST_CASE)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        UUID firstCaseUuid = UUID.fromString(firstCase.substring(1, firstCase.length() - 1));

        // retrieve case format
        mvc.perform(get(GET_CASE_FORMAT_URL, firstCaseUuid))
                .andExpect(status().isOk())
                .andExpect(content().string("XIIDM"))
                .andReturn();

        // check if the case exists (except a true)
        mvc.perform(get("/v1/cases/{caseUuid}/exists", firstCaseUuid))
                .andExpect(status().isOk())
                .andExpect(content().string("true"))
                .andReturn();

        // import a non valid case and expect a fail
        mvc.perform(multipart("/v1/cases/private")
                .file(createMockMultipartFile(NOT_A_NETWORK)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(startsWith("This file cannot be imported")))
                .andReturn();

        // import a non valid case with a valid extension and expect a fail
        mvc.perform(multipart("/v1/cases/private")
                .file(createMockMultipartFile(STILL_NOT_A_NETWORK)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(startsWith("This file cannot be imported")))
                .andReturn();

        // list the cases and expect no case since the case imported just before is not public
        mvc.perform(get("/v1/cases"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"))
                .andReturn();

        // retrieve a case as a network
        MvcResult mvcResult =  mvc.perform(get(GET_CASE_URL, firstCaseUuid)
                .param("xiidm", "false"))
                .andExpect(status().isOk())
                .andReturn();

        String testCaseContent = new String(ByteStreams.toByteArray(getClass().getResourceAsStream("/" + TEST_CASE)), StandardCharsets.UTF_8);
        mvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().xml(testCaseContent))
                .andReturn();

        // retrieve a case (async)
        mvc.perform(get(GET_CASE_URL, firstCaseUuid))
                .andExpect(status().isOk())
                .andExpect(content().xml(testCaseContent))
                .andReturn();

        // retrieve a non existing case
        mvc.perform(get(GET_CASE_URL, UUID.randomUUID()))
                .andExpect(status().isNoContent())
                .andReturn();

        // delete the case
        mvc.perform(delete(GET_CASE_URL, firstCaseUuid))
                .andExpect(status().isOk());

        // delete non existing file
        mvc.perform(delete(GET_CASE_URL, firstCaseUuid))
                .andExpect(content().string(startsWith("The directory with the following uuid doesn't exist:")))
                .andReturn();

        // import a case to delete it
        mvc.perform(multipart("/v1/cases/private")
                .file(createMockMultipartFile(TEST_CASE)))
                .andExpect(status().isOk())
                .andReturn();

        // delete all cases
        mvc.perform(delete("/v1/cases"))
                .andExpect(status().isOk());

        // import a case to public
        mvc.perform(multipart("/v1/cases/public")
                .file(createMockMultipartFile(TEST_CASE)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // list the cases and expect one case since the case imported just before is public
        mvcResult = mvc.perform(get("/v1/cases"))
                .andExpect(status().isOk())
                .andReturn();

        assertTrue(mvcResult.getResponse().getContentAsString().startsWith("[{\"name\":\"testCase.xiidm\""));
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
