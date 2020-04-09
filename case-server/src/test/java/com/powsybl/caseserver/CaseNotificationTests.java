package com.powsybl.caseserver;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import java.util.concurrent.BlockingQueue;

import static org.junit.Assert.assertThat;
import static org.springframework.cloud.stream.test.matcher.MessageQueueMatcher.receivesMessageThat;
import static org.springframework.cloud.stream.test.matcher.MessageQueueMatcher.receivesPayloadThat;
import static org.springframework.integration.test.matcher.PayloadAndHeaderMatcher.sameExceptIgnorableHeaders;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(classes = {CaseController.class, ContextFunctionCatalogAutoConfiguration.class})
@DirtiesContext
class CaseNotificationTests {

    @Autowired
    @Qualifier("publishCaseImport-out-0")
    private MessageChannel output;

    @Autowired
    private MessageCollector collector;

    @Test
    @SuppressWarnings("unchecked")
    void testMessages() {
        CaseInfos caseInfos = new CaseInfos("testCase.xml", "CGMES");

        this.output.send(caseInfos.getMessage());

        BlockingQueue<Message<?>> messages = this.collector.forChannel(this.output);

        assertThat(messages, receivesPayloadThat(CoreMatchers.is("")));

        Message<String> message = caseInfos.getMessage();
        this.output.send(message);

        Matcher<Message<Object>> sameExceptIgnorableHeaders =  (Matcher<Message<Object>>) (Matcher<?>) sameExceptIgnorableHeaders(message);
        assertThat(messages, receivesMessageThat(sameExceptIgnorableHeaders));
    }

}
