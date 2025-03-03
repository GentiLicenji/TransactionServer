package com.sisal.transaction.server.config.auth;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class HmacAuthenticationToken extends AbstractAuthenticationToken {
    private final String apiKey;
    private final String hmacSignature;
    private final RequestDetails requestDetails;  // Contains everything needed for HMAC

    /**
     * Identifies authenticity (like a password would)
     */
    @Override
    public Object getCredentials() {
        return hmacSignature;
    }

    /**
     * Identifies the client (who they are)
     */
    @Override
    public Object getPrincipal() {
        return apiKey;
    }

    public static class RequestDetails {
        private final String method;
        private final String path;
        private final String queryString;
        private final String body;
        private final String timestamp;

        public RequestDetails(String method, String path, String queryString, String body, String timestamp) {
            this.method = method;
            this.path = path;
            this.queryString = queryString;
            this.body = body;
            this.timestamp = timestamp;
        }

        public String getMethod() {
            return method;
        }

        public String getPath() {
            return path;
        }

        public String getQueryString() {
            return queryString;
        }

        public String getBody() {
            return body;
        }

        public String getTimestamp() {
            return timestamp;
        }
    }

    // Unauthenticated token constructor
    public HmacAuthenticationToken(String apiKey,
                                   String hmacSignature,
                                   RequestDetails requestDetails) {
        super(null);
        this.apiKey = apiKey;
        this.hmacSignature = hmacSignature;
        this.requestDetails = requestDetails;
        setAuthenticated(false);
    }

    // Authenticated token constructor
    public HmacAuthenticationToken(String apiKey,
                                   Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.apiKey = apiKey;
        this.hmacSignature = null;
        this.requestDetails = null;
        setAuthenticated(true);
    }

    public RequestDetails getRequestDetails() {
        return requestDetails;
    }

}