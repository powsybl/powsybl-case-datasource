/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver.elasticsearch;

import java.net.InetSocketAddress;
import java.util.Arrays;
import org.elasticsearch.client.RestHighLevelClient;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * A class to configure DB elasticsearch client for metadatas transfer
 *
 * @author Slimane Amar <slimane.amar at rte-france.com>
 */

@Configuration
@EnableElasticsearchRepositories
@Lazy
public class ESConfig extends AbstractElasticsearchConfiguration {

    @Value("#{'${spring.data.elasticsearch.embedded:false}' ? 'localhost' : '${spring.data.elasticsearch.host}'}")
    private String esHost;

    @Value("#{'${spring.data.elasticsearch.embedded:false}' ? '${spring.data.elasticsearch.embedded.port:}' : '${spring.data.elasticsearch.port}'}")
    private int esPort;

    @Bean
    @ConditionalOnExpression("'${spring.data.elasticsearch.enabled:false}' == 'true'")
    public CaseInfosService caseInfosServiceImpl() {
        return new CaseInfosServiceImpl();
    }

    @Bean
    @ConditionalOnExpression("'${spring.data.elasticsearch.enabled:false}' == 'false'")
    public CaseInfosService caseInfosServiceMock() {
        return new CaseInfosServiceMock();
    }

    @Bean
    @Override
    public RestHighLevelClient elasticsearchClient() {
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedTo(InetSocketAddress.createUnresolved(esHost, esPort))
                .build();

        return RestClients.create(clientConfiguration).rest();
    }

    @Override
    public ElasticsearchCustomConversions elasticsearchCustomConversions() {
        return new ElasticsearchCustomConversions(Arrays.asList(DateToStringConverter.INSTANCE, StringToDateConverter.INSTANCE));
    }

    @WritingConverter
    enum DateToStringConverter implements Converter<DateTime, String> {
        INSTANCE;
        @Override
        public String convert(DateTime date) {
            return date.toDateTimeISO().toString();
        }
    }

    @ReadingConverter
    enum StringToDateConverter implements Converter<String, DateTime> {
        INSTANCE;
        @Override
        public DateTime convert(String s) {
            DateTimeFormatter parser = ISODateTimeFormat.dateTimeParser();
            return parser.parseDateTime(s);
        }
    }

}
