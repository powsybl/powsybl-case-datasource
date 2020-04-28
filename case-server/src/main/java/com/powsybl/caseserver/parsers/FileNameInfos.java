package com.powsybl.caseserver.parsers;

public interface FileNameInfos {

    public enum Type {
        ENTSOE
    }

    Type getType();
}
