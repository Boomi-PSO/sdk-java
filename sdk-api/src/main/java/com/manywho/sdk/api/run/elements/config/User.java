package com.manywho.sdk.api.run.elements.config;

public class User {
    private String authenticationId;
    private String attribute;
    private boolean runningUser;

    public String getAuthenticationId() {
        return authenticationId;
    }

    public void setAuthenticationId(String authenticationId) {
        this.authenticationId = authenticationId;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public boolean isRunningUser() {
        return runningUser;
    }

    public void setRunningUser(boolean runningUser) {
        this.runningUser = runningUser;
    }
}
