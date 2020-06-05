package com.powsybl.caseserver.parsers;

public interface FileNameInfos {

    enum Type {
        ENTSOE
    }

    Type getType();
}
