package com.tecsup.dto;

import java.util.List;

public class JwtResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private Long id;
    private String username;
    private String email;
    private List<String> roles;

    public JwtResponse(Long id, String username, String email, String accessToken, List<String> roles) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.accessToken = accessToken;
        this.roles = roles;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public List<String> getRoles() { return roles; }
}
