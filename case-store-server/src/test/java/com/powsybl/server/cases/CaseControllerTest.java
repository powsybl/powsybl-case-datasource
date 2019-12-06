package com.powsybl.server.cases;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.xml.NetworkXml;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static com.powsybl.server.cases.CaseConstants.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@EnableWebMvc
@WebMvcTest
public class CaseControllerTest {

    @Autowired
    private MockMvc mvc;

    @BeforeClass
    public static void setup() {
        final Path path = Paths.get(System.getProperty(USERHOME) + CASE_FOLDER);
        if (Files.notExists(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                e.printStackTrace();
                fail();
            }
        }
    }

    @Test
    public void test() throws Exception {
        mvc.perform(delete("/v1/case-server/cases"))
                .andExpect(status().isOk());

        //Import a case
        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream("testCase.xiidm")) {
            MockMultipartFile mockFile = new MockMultipartFile("file", "testCase.xiidm", "text/plain", inputStream);
            MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.multipart("/v1/case-server/import-case")
                    .file(mockFile))
                    .andExpect(status().isOk())
                    .andReturn();
            assertEquals(DONE, mvcResult.getResponse().getContentAsString());
        }

        //Import the same case and expect a fail
        try (InputStream inputStream = classLoader.getResourceAsStream("testCase.xiidm")) {
            MockMultipartFile mockFile = new MockMultipartFile("file", "testCase.xiidm", "text/plain", inputStream);
            MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.multipart("/v1/case-server/import-case")
                    .file(mockFile))
                    .andExpect(status().is(409))
                    .andReturn();
            assertEquals(FILE_ALREADY_EXISTS, mvcResult.getResponse().getErrorMessage());

        }

        //List the cases and expect the case added just before
        MvcResult mvcResult = mvc.perform(get("/v1/case-server/cases"))
                .andExpect(status().isOk())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        String tmp = mvcResult.getResponse().getContentAsString();
        Map<String, String> listCase = objectMapper.readValue(tmp, Map.class);
        assertTrue(listCase.containsValue("testCase.xiidm"));
        assertTrue(listCase.containsKey(System.getProperty(USERHOME) + CASE_FOLDER + "testCase.xiidm"));

        //Retrieve a case as a network
        mvcResult =  mvc.perform(get("/v1/case-server/download-network")
        .param("caseName", "testCase.xiidm"))
                .andExpect(status().isOk())
                .andReturn();
        Network network = NetworkXml.gunzip(mvcResult.getResponse().getContentAsByteArray());
        assertEquals("20140116_0830_2D4_UX1_pst", network.getName());

        //Retrieve a case (async)
        mvcResult = mvc.perform(get("/v1/case-server/cases/{caseName}", "testCase.xiidm"))
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

        //Delete the case
        mvc.perform(delete("/v1/case-server/cases/{caseName}", "testCase.xiidm"))
                .andExpect(status().isOk());

        //Delete non existing file
        mvcResult = mvc.perform(delete("/v1/case-server/cases/{caseName}", "testCase.xiidm"))
                .andReturn();

        assertEquals(FILE_DOESNT_EXIST, mvcResult.getResponse().getErrorMessage());
    }
}
