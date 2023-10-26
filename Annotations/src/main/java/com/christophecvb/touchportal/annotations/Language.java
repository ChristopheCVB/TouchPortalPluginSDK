package com.christophecvb.touchportal.annotations;

/**
 * Languages supported by Touch Portal
 */
public enum Language {
    //ENGLISH("en"), This is the default language to provide in Action annotation
    GERMAN("de"),
    SPANISH("es"),
    FRENCH("fr"),
    DUTCH("nl"),
    PORTUGUESE("pt"),
    TURKISH("tr");


    private final String code;

    Language(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
