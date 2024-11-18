package com.christophecvb.touchportal.annotations;

public enum ValueType {
    TEXT("text"),
    CHOICE("choice")
    ;

    private final String key;

    ValueType(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }
}
