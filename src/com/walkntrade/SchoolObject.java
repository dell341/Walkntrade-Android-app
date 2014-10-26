package com.walkntrade;

/**
 * Created by Nate on 10/25/2014.
 */
public class SchoolObject {

    private String fullName, shortName;

    public SchoolObject(String fullName, String shortName) {
        this.fullName = fullName;
        this.shortName = shortName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getShortName() {
        return shortName;
    }
}
