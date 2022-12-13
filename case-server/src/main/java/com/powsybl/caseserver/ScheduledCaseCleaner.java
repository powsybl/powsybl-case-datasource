/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver;

import com.powsybl.caseserver.repository.CaseMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Service
public class ScheduledCaseCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledCaseCleaner.class);

    private final CaseMetadataRepository caseMetadataRepository;

    private final CaseService caseService;

    public ScheduledCaseCleaner(CaseMetadataRepository caseMetadataRepository, CaseService caseService) {
        this.caseMetadataRepository = caseMetadataRepository;
        this.caseService = caseService;
    }

    @Scheduled(cron = "${cleaning-cases-cron}", zone = "UTC")
    public void deleteExpiredCases() {
        LocalDateTime localDateTime = LocalDateTime.now(ZoneOffset.UTC);
        LOGGER.info("Cleaning cases cron starting execution at {}", localDateTime);
        caseMetadataRepository.findAll().stream().filter(caseMetadataEntity -> caseMetadataEntity.getExpirationDate() != null)
                .filter(caseMetadataEntity -> localDateTime.isAfter(caseMetadataEntity.getExpirationDate()))
                .forEach(caseMetadataEntity -> {
                    caseService.deleteCase(caseMetadataEntity.getId());
                    caseMetadataRepository.deleteById(caseMetadataEntity.getId());
                });
    }
}
