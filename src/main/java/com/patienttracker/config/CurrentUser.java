package com.patienttracker.config;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Holds the username for the current HTTP request.
 * Set by controllers from the X-Username request header.
 * @RequestScope means a fresh instance per request — no thread-safety concerns.
 */
@Component
@RequestScope
public class CurrentUser {

    private String username = "staff"; // default for backwards compatibility

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}