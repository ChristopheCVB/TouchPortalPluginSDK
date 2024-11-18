package com.christophecvb.touchportal.annotations;

/**
 * Parent Categories supported by Touch Portal
 */
public enum ParentCategory {
    AUDIO("audio"),
    STREAMING("streaming"),
    CONTENT("content"),
    HOME_AUTOMATION("homeautomation"),
    SOCIAL("social "),
    GAMES("games"),
    MISC("misc");


    private final String key;

    ParentCategory(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }
}
