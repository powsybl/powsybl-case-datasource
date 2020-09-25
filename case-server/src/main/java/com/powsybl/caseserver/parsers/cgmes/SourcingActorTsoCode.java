/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.caseserver.parsers.cgmes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public enum SourcingActorTsoCode {

    FR("RTEFRANCE", "RTEFRANCE-FR"),
    ES("REE", "REE-ES"),
    PT("REN", "REN-PT"),
    BE("BE"),
    NL("NL"),
    UNDEFINED();

    private static final Map<String, SourcingActorTsoCode> BY_SOURCING_ACTOR = new HashMap<>();

    static {
        for (SourcingActorTsoCode c : values()) {
            for (String s : c.sourcingActors) {
                BY_SOURCING_ACTOR.put(s, c);
            }
        }
    }

    private List<String> sourcingActors;

    SourcingActorTsoCode(String... sourcingActors) {
        this.sourcingActors = Arrays.asList(sourcingActors);
    }

    public static SourcingActorTsoCode tsoFromSourcingActor(String sourcingActor) {
        SourcingActorTsoCode tsoCode = BY_SOURCING_ACTOR.get(sourcingActor);
        return tsoCode != null ? tsoCode : UNDEFINED;
    }
}
