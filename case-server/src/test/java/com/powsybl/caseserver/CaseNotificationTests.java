/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.caseserver;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertThat;
import static org.springframework.cloud.stream.test.matcher.MessageQueueMatcher.receivesMessageThat;
import static org.springframework.cloud.stream.test.matcher.MessageQueueMatcher.receivesPayloadThat;
import static org.springframework.integration.test.matcher.PayloadAndHeaderMatcher.sameExceptIgnorableHeaders;

/**
 * @author Slimane Amar <slimane.amar at rte-france.com>
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,  classes = CaseController.class)
public class CaseNotificationTests {

    private static final String TEST_UCTE_CASE_FILE_NAME = "20200430_1530_2D4_D41.uct";

    @Autowired
    private CaseService caseService;

    @Autowired
    @Qualifier("publishCaseImport-out-0")
    private MessageChannel output;

    @Autowired
    private MessageCollector collector;

    @Test
    @SuppressWarnings("unchecked")
    public void testMessages() {
        Path casePath = Path.of(this.getClass().getResource("/" + TEST_UCTE_CASE_FILE_NAME).getPath());
        String fileBaseName = casePath.getFileName().toString();
        String format = caseService.getFormat(casePath);
        CaseInfos caseFileName = caseService.createInfos(fileBaseName, UUID.randomUUID(), format);
        this.output.send(caseFileName.createMessage());

        BlockingQueue<Message<?>> messages = this.collector.forChannel(this.output);
        assertThat(messages, receivesPayloadThat(CoreMatchers.is("")));

        caseFileName = caseService.createInfos(fileBaseName, UUID.randomUUID(), format);
        Message<String> message = caseFileName.createMessage();
        this.output.send(message);

        Matcher<Message<Object>> sameExceptIgnorableHeaders = (Matcher<Message<Object>>) (Matcher<?>) sameExceptIgnorableHeaders(message);
        assertThat(messages, receivesMessageThat(sameExceptIgnorableHeaders));
    }

}
