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
import com.powsybl.caseserver.dto.CaseInfos;
import com.powsybl.caseserver.parsers.entsoe.EntsoeFileNameParser;
import com.powsybl.caseserver.repository.CaseMetadataEntity;
import com.powsybl.caseserver.repository.CaseMetadataRepository;
import com.powsybl.computation.ComputationManager;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {"spring.data.elasticsearch.enabled=true"},
        classes = { EmbeddedElasticsearch.class, CaseController.class, TestChannelBinderConfiguration.class})
@TestPropertySource(properties = {"case-store-directory=/cases"})
public class CaseControllerTest {

    private static final String TEST_CASE = "testCase.xiidm";
    private static final String TEST_CASE_FORMAT = "XIIDM";
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

    @Autowired
    private CaseMetadataRepository caseMetadataRepository;

    @Autowired
    private OutputDestination outputDestination;

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
        UUID firstCaseUuid = importCase(TEST_CASE, false);

        // assert that the broker message has been sent
        Message<byte[]> messageImport = outputDestination.receive(1000, "case.import.destination");
        assertEquals("", new String(messageImport.getPayload()));
        MessageHeaders headersCase = messageImport.getHeaders();
        assertEquals("testCase.xiidm", headersCase.get(CaseInfos.NAME_HEADER_KEY));
        assertEquals(firstCaseUuid, headersCase.get(CaseInfos.UUID_HEADER_KEY));
        assertEquals("XIIDM", headersCase.get(CaseInfos.FORMAT_HEADER_KEY));

        //check that the case doesn't have an expiration date
        CaseMetadataEntity caseMetadataEntity = caseMetadataRepository.findById(firstCaseUuid).orElseThrow();
        assertEquals(firstCaseUuid, caseMetadataEntity.getId());
        assertNull(caseMetadataEntity.getExpirationDate());

        // retrieve case format
        mvc.perform(get(GET_CASE_FORMAT_URL, firstCaseUuid))
                .andExpect(status().isOk())
                .andExpect(content().string(TEST_CASE_FORMAT))
                .andReturn();

        //retrieve case name
        mvc.perform(get("/v1/cases/{caseUuid}/name", firstCaseUuid))
                .andExpect(status().isOk())
                .andExpect(content().string(TEST_CASE))
                .andReturn();

        //retrieve unknown case name
        mvc.perform(get("/v1/cases/{caseUuid}/name", UUID.randomUUID()))
                .andExpect(status().is5xxServerError());

        //retrieve case infos
        mvc.perform(get("/v1/cases/{caseUuid}/infos", firstCaseUuid))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value(TEST_CASE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.format").value(TEST_CASE_FORMAT))
                .andReturn();

        //retrieve case infos
        mvc.perform(get("/v1/cases/{caseUuid}/infos", UUID.randomUUID()))
                .andExpect(status().isNoContent())
                .andReturn();

        // check if the case exists (except a true)
        mvc.perform(get("/v1/cases/{caseUuid}/exists", firstCaseUuid))
                .andExpect(status().isOk())
                .andExpect(content().string("true"))
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

        // list the cases and expect the one imported before
        mvc.perform(get("/v1/cases"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$", hasSize(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].name").value(TEST_CASE))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].format").value(TEST_CASE_FORMAT))
                .andReturn();

        // retrieve a case as a network
        String testCaseContent = new String(ByteStreams.toByteArray(getClass().getResourceAsStream("/" + TEST_CASE)), StandardCharsets.UTF_8);
        mvc.perform(get(GET_CASE_URL, firstCaseUuid)
                .param("xiidm", "false"))
                .andExpect(status().isOk())
                .andExpect(content().xml(testCaseContent))
                .andReturn();

        // retrieve a case
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
        UUID secondCaseUuid = importCase(TEST_CASE, false);

        // assert that the broker message has been sent
        Message<byte[]> messageImportPrivate2 = outputDestination.receive(1000, "case.import.destination");
        assertEquals("", new String(messageImportPrivate2.getPayload()));
        MessageHeaders headersPrivateCase2 = messageImportPrivate2.getHeaders();
        assertEquals("testCase.xiidm", headersPrivateCase2.get(CaseInfos.NAME_HEADER_KEY));
        assertEquals(secondCaseUuid, headersPrivateCase2.get(CaseInfos.UUID_HEADER_KEY));
        assertEquals("XIIDM", headersPrivateCase2.get(CaseInfos.FORMAT_HEADER_KEY));

        //check that the case doesn't have an expiration date
        caseMetadataEntity = caseMetadataRepository.findById(secondCaseUuid).orElseThrow();
        assertEquals(secondCaseUuid, caseMetadataEntity.getId());
        assertNull(caseMetadataEntity.getExpirationDate());

        // delete all cases
        mvc.perform(delete("/v1/cases"))
                .andExpect(status().isOk());

        //check that the caseMetadataRepository is empty since all cases were removed
        assertTrue(caseMetadataRepository.findAll().isEmpty());

        UUID caseUuid = importCase(TEST_CASE, false);

        // assert that the broker message has been sent
        messageImport = outputDestination.receive(1000, "case.import.destination");
        assertEquals("", new String(messageImport.getPayload()));
        headersCase = messageImport.getHeaders();
        assertEquals("testCase.xiidm", headersCase.get(CaseInfos.NAME_HEADER_KEY));
        assertEquals(caseUuid, headersCase.get(CaseInfos.UUID_HEADER_KEY));
        assertEquals("XIIDM", headersCase.get(CaseInfos.FORMAT_HEADER_KEY));

        //check that the case doesn't have an expiration date
        caseMetadataEntity = caseMetadataRepository.findById(caseUuid).orElseThrow();
        assertEquals(caseUuid, caseMetadataEntity.getId());
        assertNull(caseMetadataEntity.getExpirationDate());

        //duplicate an existing case
        MvcResult duplicateResult = mvc.perform(post("/v1/cases").param("duplicateFrom", caseUuid.toString()))
                .andExpect(status().isOk())
                .andReturn();

        String duplicateCaseUuid = duplicateResult.getResponse().getContentAsString().replace("\"", "");

        // assert that broker message has been sent after duplication
        messageImport = outputDestination.receive(1000, "case.import.destination");
        assertEquals("", new String(messageImport.getPayload()));
        headersCase = messageImport.getHeaders();
        assertEquals(UUID.fromString(duplicateCaseUuid), headersCase.get(CaseInfos.UUID_HEADER_KEY));
        assertEquals("testCase.xiidm", headersCase.get(CaseInfos.NAME_HEADER_KEY));
        assertEquals("XIIDM", headersCase.get(CaseInfos.FORMAT_HEADER_KEY));

        //check that the duplicated case doesn't have an expiration date
        caseMetadataEntity = caseMetadataRepository.findById(UUID.fromString(duplicateCaseUuid)).orElseThrow();
        assertEquals(duplicateCaseUuid, caseMetadataEntity.getId().toString());
        assertNull(caseMetadataEntity.getExpirationDate());

        // import a case with expiration
        LocalDateTime beforeImportDate = LocalDateTime.now(ZoneOffset.UTC).plusHours(1);
        UUID thirdCaseUuid = importCase(TEST_CASE, true);
        LocalDateTime afterImportDate = LocalDateTime.now(ZoneOffset.UTC).plusHours(1);

        // assert that the broker message has been sent
        messageImport = outputDestination.receive(1000, "case.import.destination");
        assertEquals("", new String(messageImport.getPayload()));
        headersCase = messageImport.getHeaders();
        assertEquals("testCase.xiidm", headersCase.get(CaseInfos.NAME_HEADER_KEY));
        assertEquals(thirdCaseUuid, headersCase.get(CaseInfos.UUID_HEADER_KEY));
        assertEquals("XIIDM", headersCase.get(CaseInfos.FORMAT_HEADER_KEY));

        //check that the case does have an expiration date
        caseMetadataEntity = caseMetadataRepository.findById(thirdCaseUuid).orElseThrow();
        assertEquals(thirdCaseUuid, caseMetadataEntity.getId());
        //verify that beforeImportDate < caseMetadataEntity.getExpirationDate() < afterImportDate
        assertTrue(caseMetadataEntity.getExpirationDate().isAfter(beforeImportDate));
        assertTrue(caseMetadataEntity.getExpirationDate().isBefore(afterImportDate));
        assertNotNull(caseMetadataEntity.getExpirationDate());

        //duplicate an existing case withExpiration
        MvcResult duplicateResult2 = mvc.perform(post("/v1/cases")
                .param("duplicateFrom", caseUuid.toString())
                .param("withExpiration", "true"))
                .andExpect(status().isOk())
                .andReturn();

        String duplicateCaseUuid2 = duplicateResult2.getResponse().getContentAsString().replace("\"", "");
        assertNotEquals(caseUuid.toString(), duplicateCaseUuid2);

        // assert that broker message has been sent after duplication
        messageImport = outputDestination.receive(1000, "case.import.destination");
        assertEquals("", new String(messageImport.getPayload()));
        headersCase = messageImport.getHeaders();
        assertEquals(UUID.fromString(duplicateCaseUuid2), headersCase.get(CaseInfos.UUID_HEADER_KEY));
        assertEquals("testCase.xiidm", headersCase.get(CaseInfos.NAME_HEADER_KEY));
        assertEquals("XIIDM", headersCase.get(CaseInfos.FORMAT_HEADER_KEY));

        //check that the duplicated case does have an expiration date
        caseMetadataEntity = caseMetadataRepository.findById(UUID.fromString(duplicateCaseUuid2)).orElseThrow();
        assertEquals(duplicateCaseUuid2, caseMetadataEntity.getId().toString());
        assertNotNull(caseMetadataEntity.getExpirationDate());

        //remove the expiration date of the previously duplicated case
        mvc.perform(delete("/v1/cases/{caseUuid}/expiration", duplicateCaseUuid2))
                .andExpect(status().isOk())
                .andReturn();

        //verify that the expiration date is removed
        caseMetadataEntity = caseMetadataRepository.findById(UUID.fromString(duplicateCaseUuid2)).orElseThrow();
        assertNull(caseMetadataEntity.getExpirationDate());

        //remove the duplicated case and check that the entry is deleted from the CaseMetadataRepository
        mvc.perform(delete("/v1/cases/{caseUuid}", duplicateCaseUuid2))
                .andExpect(status().isOk())
                .andReturn();

        assertTrue(caseMetadataRepository.findById(UUID.fromString(duplicateCaseUuid2)).isEmpty());

        //remove the expiration date of a non existing case and expect a 404
        UUID randomUuid = UUID.randomUUID();
        MvcResult deleteExpirationResult = mvc.perform(delete("/v1/cases/{caseUuid}/expiration", randomUuid))
                .andExpect(status().isNotFound())
                .andReturn();
        assertTrue(deleteExpirationResult.getResponse().getErrorMessage().contains("case " + randomUuid + " not found"));

        // assert that duplicating a non existing case should return a 404
        mvc.perform(post("/v1/cases").param("duplicateFrom", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound())
                .andReturn();

        // list the cases and expect one case
        MvcResult mvcResult = mvc.perform(get("/v1/cases"))
                .andExpect(status().isOk())
                .andReturn();

        assertTrue(mvcResult.getResponse().getContentAsString().contains("\"name\":\"testCase.xiidm\""));
    }

    private UUID importCase(String testCase, Boolean withExpiration) throws Exception {
        String importedCase;
        if (withExpiration) {
            importedCase = mvc.perform(multipart("/v1/cases")
                    .file(createMockMultipartFile(testCase))
                    .param("withExpiration", withExpiration.toString()))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
        } else {
            importedCase = mvc.perform(multipart("/v1/cases")
                    .file(createMockMultipartFile(testCase)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
        }
        return UUID.fromString(importedCase.substring(1, importedCase.length() - 1));
    }

    @Test
    public void validateCaseNameTest() {
        CaseService.validateCaseName("test");
        CaseService.validateCaseName("test.xiidm");
        CaseService.validateCaseName("te-st");
        CaseService.validateCaseName("test-case.7zip");
        CaseService.validateCaseName("testcase1.7zip");
        CaseService.validateCaseName("testcase1.xiidm.gz");

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

    @Test
    public void searchCaseTest() throws Exception {
        // create the storage dir
        createStorageDir();

        // delete all cases
        mvc.perform(delete("/v1/cases"))
                .andExpect(status().isOk());

        // import IIDM test case
        String aCase = mvc.perform(multipart("/v1/cases")
                .file(createMockMultipartFile("testCase.xiidm")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        UUID aCaseUuid = UUID.fromString(aCase.substring(1, aCase.length() - 1));

        // assert that broker message has been sent and properties are the right ones
        Message<byte[]> messageImport = outputDestination.receive(1000, "case.import.destination");
        assertEquals("", new String(messageImport.getPayload()));
        MessageHeaders headersCase = messageImport.getHeaders();
        assertEquals("testCase.xiidm", headersCase.get(CaseInfos.NAME_HEADER_KEY));
        assertEquals(aCaseUuid, headersCase.get(CaseInfos.UUID_HEADER_KEY));
        assertEquals("XIIDM", headersCase.get(CaseInfos.FORMAT_HEADER_KEY));

        // import CGMES french file
        aCase = mvc.perform(multipart("/v1/cases")
                .file(createMockMultipartFile("20200424T1330Z_2D_RTEFRANCE_001.zip")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        aCaseUuid = UUID.fromString(aCase.substring(1, aCase.length() - 1));

        // assert that broker message has been sent and properties are the right ones
        messageImport = outputDestination.receive(1000, "case.import.destination");
        assertEquals("", new String(messageImport.getPayload()));
        headersCase = messageImport.getHeaders();
        assertEquals("20200424T1330Z_2D_RTEFRANCE_001.zip", headersCase.get(CaseInfos.NAME_HEADER_KEY));
        assertEquals(aCaseUuid, headersCase.get(CaseInfos.UUID_HEADER_KEY));
        assertEquals("CGMES", headersCase.get(CaseInfos.FORMAT_HEADER_KEY));

        // import UCTE french file
        aCase = mvc.perform(multipart("/v1/cases")
                .file(createMockMultipartFile("20200103_0915_FO5_FR0.UCT")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        aCaseUuid = UUID.fromString(aCase.substring(1, aCase.length() - 1));

        // assert that broker message has been sent and properties are the right ones
        messageImport = outputDestination.receive(1000, "case.import.destination");
        assertEquals("", new String(messageImport.getPayload()));
        headersCase = messageImport.getHeaders();
        assertEquals("20200103_0915_FO5_FR0.UCT", headersCase.get(CaseInfos.NAME_HEADER_KEY));
        assertEquals(aCaseUuid, headersCase.get(CaseInfos.UUID_HEADER_KEY));
        assertEquals("UCTE", headersCase.get(CaseInfos.FORMAT_HEADER_KEY));

        // import UCTE german file
        aCase = mvc.perform(multipart("/v1/cases")
                .file(createMockMultipartFile("20200103_0915_SN5_D80.UCT")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        aCaseUuid = UUID.fromString(aCase.substring(1, aCase.length() - 1));

        // assert that broker message has been sent and properties are the right ones
        messageImport = outputDestination.receive(1000, "case.import.destination");
        assertEquals("", new String(messageImport.getPayload()));
        headersCase = messageImport.getHeaders();
        assertEquals("20200103_0915_SN5_D80.UCT", headersCase.get(CaseInfos.NAME_HEADER_KEY));
        assertEquals(aCaseUuid, headersCase.get(CaseInfos.UUID_HEADER_KEY));
        assertEquals("UCTE", headersCase.get(CaseInfos.FORMAT_HEADER_KEY));

        // import UCTE swiss file
        aCase = mvc.perform(multipart("/v1/cases")
                .file(createMockMultipartFile("20200103_0915_135_CH2.UCT")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        aCaseUuid = UUID.fromString(aCase.substring(1, aCase.length() - 1));

        // assert that broker message has been sent and properties are the right ones
        messageImport = outputDestination.receive(1000, "case.import.destination");
        assertEquals("", new String(messageImport.getPayload()));
        headersCase = messageImport.getHeaders();
        assertEquals("20200103_0915_135_CH2.UCT", headersCase.get(CaseInfos.NAME_HEADER_KEY));
        assertEquals(aCaseUuid, headersCase.get(CaseInfos.UUID_HEADER_KEY));
        assertEquals("UCTE", headersCase.get(CaseInfos.FORMAT_HEADER_KEY));

        // list the cases
        MvcResult mvcResult = mvc.perform(get("/v1/cases"))
                .andExpect(status().isOk())
                .andReturn();

        // assert that the 5 previously imported cases are present
        String response = mvcResult.getResponse().getContentAsString();
        assertTrue(response.contains("\"name\":\"testCase.xiidm\""));
        assertTrue(response.contains("\"name\":\"20200424T1330Z_2D_RTEFRANCE_001.zip\""));
        assertTrue(response.contains("\"name\":\"20200103_0915_FO5_FR0.UCT\""));
        assertTrue(response.contains("\"name\":\"20200103_0915_SN5_D80.UCT\""));
        assertTrue(response.contains("\"name\":\"20200103_0915_135_CH2.UCT\""));

        // search the cases
        mvcResult = mvc.perform(get("/v1/cases/search")
                .param("q", "*"))
                .andExpect(status().isOk())
                .andReturn();
        response = mvcResult.getResponse().getContentAsString();
        assertTrue(response.contains("\"name\":\"testCase.xiidm\""));
        assertTrue(response.contains("\"name\":\"20200424T1330Z_2D_RTEFRANCE_001.zip\""));
        assertTrue(response.contains("\"name\":\"20200103_0915_FO5_FR0.UCT\""));
        assertTrue(response.contains("\"name\":\"20200103_0915_SN5_D80.UCT\""));
        assertTrue(response.contains("\"name\":\"20200103_0915_135_CH2.UCT\""));

        String t = getDateSearchTerm("20200103_0915");
        mvcResult = mvc.perform(get("/v1/cases/search")
                .param("q", getDateSearchTerm("20200103_0915")))
                .andExpect(status().isOk())
                .andReturn();
        response = mvcResult.getResponse().getContentAsString();
        assertFalse(response.contains("\"name\":\"testCase.xiidm\""));
        assertFalse(response.contains("\"name\":\"20200424T1330Z_2D_RTEFRANCE_001.zip\""));
        assertTrue(response.contains("\"name\":\"20200103_0915_FO5_FR0.UCT\""));
        assertTrue(response.contains("\"name\":\"20200103_0915_SN5_D80.UCT\""));
        assertTrue(response.contains("\"name\":\"20200103_0915_135_CH2.UCT\""));

        mvcResult = mvc.perform(get("/v1/cases/search")
                .param("q", "geographicalCode:(FR) OR tso:(RTEFRANCE)"))
                .andExpect(status().isOk())
                .andReturn();
        response = mvcResult.getResponse().getContentAsString();
        assertFalse(response.contains("\"name\":\"testCase.xiidm\""));
        assertTrue(response.contains("\"name\":\"20200424T1330Z_2D_RTEFRANCE_001.zip\""));
        assertTrue(response.contains("\"name\":\"20200103_0915_FO5_FR0.UCT\""));
        assertFalse(response.contains("\"name\":\"20200103_0915_SN5_D80.UCT\""));
        assertFalse(response.contains("\"name\":\"20200103_0915_135_CH2.UCT\""));

        mvcResult = mvc.perform(get("/v1/cases/search")
                .param("q",  getDateSearchTerm("20140116_0830") + " AND geographicalCode:(ES)"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("[]", mvcResult.getResponse().getContentAsString());

        mvcResult = mvc.perform(get("/v1/cases/search")
                .param("q", getDateSearchTerm("20140116_0830") + " AND geographicalCode:(FR)"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("[]", mvcResult.getResponse().getContentAsString());

        mvcResult = mvc.perform(get("/v1/cases/search")
                .param("q", getDateSearchTerm("20200212_1030") + " AND geographicalCode:(PT)"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("[]", mvcResult.getResponse().getContentAsString());

        mvcResult = mvc.perform(get("/v1/cases/search")
                .param("q", getDateSearchTerm("20200212_1030") + " AND geographicalCode:(FR)"))
                .andExpect(status().isOk())
                .andReturn();
        response = mvcResult.getResponse().getContentAsString();
        assertFalse(response.contains("\"name\":\"20200424T1330Z_2D_RTEFRANCE_001.zip\""));
        assertFalse(response.contains("\"name\":\"20200103_0915_FO5_FR0.UCT\""));
        assertFalse(response.contains("\"name\":\"20200103_0915_SN5_D80.UCT\""));
        assertFalse(response.contains("\"name\":\"20200103_0915_135_CH2.UCT\""));

        mvcResult = mvc.perform(get("/v1/cases/search")
                .param("q", getDateSearchTerm("20200103_0915") + " AND geographicalCode:(CH)"))
                .andExpect(status().isOk())
                .andReturn();
        response = mvcResult.getResponse().getContentAsString();
        assertFalse(response.contains("\"name\":\"20200424T1330Z_2D_RTEFRANCE_001.zip\""));
        assertFalse(response.contains("\"name\":\"20200103_0915_FO5_FR0.UCT\""));
        assertFalse(response.contains("\"name\":\"20200103_0915_SN5_D80.UCT\""));
        assertTrue(response.contains("\"name\":\"20200103_0915_135_CH2.UCT\""));

        mvcResult = mvc.perform(get("/v1/cases/search")
                .param("q", getDateSearchTerm("20200103_0915") + " AND geographicalCode:(FR OR CH OR D8)"))
                .andExpect(status().isOk())
                .andReturn();
        response = mvcResult.getResponse().getContentAsString();
        assertTrue(response.contains("\"name\":\"20200103_0915_FO5_FR0.UCT\""));
        assertTrue(response.contains("\"name\":\"20200103_0915_SN5_D80.UCT\""));
        assertTrue(response.contains("\"name\":\"20200103_0915_135_CH2.UCT\""));
        assertFalse(response.contains("\"name\":\"20200424T1330Z_2D_RTEFRANCE_001.zip\""));

        mvcResult = mvc.perform(get("/v1/cases/search")
                .param("q", "tso:(RTEFRANCE) AND businessProcess:(2D) AND format:(CGMES)"))
                .andExpect(status().isOk())
                .andReturn();
        response = mvcResult.getResponse().getContentAsString();
        assertFalse(response.contains("\"name\":\"testCase.xiidm\""));
        assertTrue(response.contains("\"name\":\"20200424T1330Z_2D_RTEFRANCE_001.zip\""));
        assertFalse(response.contains("\"name\":\"20200103_0915_FO5_FR0.UCT\""));
        assertFalse(response.contains("\"name\":\"20200103_0915_SN5_D80.UCT\""));
        assertFalse(response.contains("\"name\":\"20200103_0915_135_CH2.UCT\""));

        // reindex all cases
        mvc.perform(post("/v1/cases/reindex-all"))
            .andExpect(status().isOk());

        mvcResult = mvc.perform(get("/v1/cases/search")
                .param("q", "*"))
                .andExpect(status().isOk())
                .andReturn();
        response = mvcResult.getResponse().getContentAsString();
        assertTrue(response.contains("\"name\":\"testCase.xiidm\""));
        assertTrue(response.contains("\"name\":\"20200424T1330Z_2D_RTEFRANCE_001.zip\""));
        assertTrue(response.contains("\"name\":\"20200103_0915_FO5_FR0.UCT\""));
        assertTrue(response.contains("\"name\":\"20200103_0915_SN5_D80.UCT\""));
        assertTrue(response.contains("\"name\":\"20200103_0915_135_CH2.UCT\""));

        // delete all cases
        mvc.perform(delete("/v1/cases"))
                .andExpect(status().isOk());

        mvcResult = mvc.perform(get("/v1/cases/search")
                .param("q", getDateSearchTerm("20200103_0915") + " AND geographicalCode:(FR OR CH OR D8)"))
                .andExpect(status().isOk())
                .andReturn();
        response = mvcResult.getResponse().getContentAsString();
        assertFalse(response.contains("\"name\":\"20200103_0915_FO5_FR0.UCT\""));
        assertFalse(response.contains("\"name\":\"20200103_0915_SN5_D80.UCT\""));
        assertFalse(response.contains("\"name\":\"20200103_0915_135_CH2.UCT\""));
        assertFalse(response.contains("\"name\":\"20200424T1330Z_2D_RTEFRANCE_001.zip\""));
    }

    private String getDateSearchTerm(String entsoeFormatDate) {
        String utcFormattedDate = EntsoeFileNameParser.parseDateTime(entsoeFormatDate).toDateTimeISO().toString();
        return "date:\"" + utcFormattedDate + "\"";
    }
}
