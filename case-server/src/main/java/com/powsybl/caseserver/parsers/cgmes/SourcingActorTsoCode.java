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
    D8("50Hertz", "50Hertz-D8"),
    GR("IPTO", "Admie-GR"),
    D7("D7", "Amprion-D7"),
    AT("APG", "APG-AT"),
    LV("AST", "AST-LV"),
    CZ("CEPS", "CEPS-CZ"),
    ME("CGES", "CGES-ME"),
    LU("CREOS", "CREOS-LU"),
    NIIE("EIRGRIDSONI", "EIRGRIDSONI-NIIE"),
    EE("ELERING", "ELERING-EE"),
    SI("ELES", "ELES-SI"),
    BE("ELIA", "ELIA-BE"),
    RS("EMS", "EMS-RS"),
    DKE("DKE", "Energinet-DKE"),
    DKW("DKW", "Energinet-DKW"),
    Kontiscan("Energinet-Kontiscan"),
    BG("ESO", "ESO-BG"),
    FI("FI", "Fingrid-SI"),
    HR("HOPS", "HOPS-HR"),
    KS("KOSTT-KS"),
    LT("LITGRID", "LITGRID-LT"),
    HU("MAVIR", "MAVIR-HU"),
    MK("MEPSO", "MEPSO-MK"),
    GB("NG", "Nationalgrideso-GB"),
    BA("NOSBIH", "NOSBIH-BA"),
    AL("OST", "OST-AL"),
    PL("PSE", "PSE-PL"),
    ES("REE", "REE-ES"),
    PT("REN", "REN-PT"),
    FR("RTEFRANCE", "RTEFRANCE-FR"),
    SK("SEPS", "SEPS-SK"),
    NO("STATNETT", "STATNETT-NO"),
    SE("SVK", "SVK-SE"),
    CH("SWISSGRID", "SWISSGRID-CH"),
    TR("TEIAS", "TEIAS-TR"),
    D2("TTG", "TenneTDE-D2"),
    NL("TTN", "TenneTNL-NL"),
    IT("TERNA", "TERNA-IT"),
    RO("TRANSELECTRICA", "TRANSELECTRICA-RO"),
    UA("Ukrenergo", "Ukrenergo-UA"),
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
