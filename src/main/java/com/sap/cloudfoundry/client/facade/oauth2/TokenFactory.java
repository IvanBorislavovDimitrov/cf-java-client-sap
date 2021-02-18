package com.sap.cloudfoundry.client.facade.oauth2;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.server.ResponseStatusException;

import com.sap.cloudfoundry.client.facade.util.JsonUtil;

public class TokenFactory {

    // Scopes:
    public static final String SCOPE_CC_READ = "cloud_controller.read";
    public static final String SCOPE_CC_WRITE = "cloud_controller.write";
    public static final String SCOPE_CC_ADMIN = "cloud_controller.admin";
    public static final String SCOPE_SCIM_USERIDS = "scim.userids";
    public static final String SCOPE_PASSWORD_WRITE = "password.write";
    public static final String SCOPE_OPENID = "openid";

    // Token Body elements:
    public static final String SCOPE = "scope";
    public static final String EXP = "exp";
    public static final String IAT = "iat";
    public static final String USER_NAME = "user_name";
    public static final String USER_ID = "user_id";
    public static final String CLIENT_ID = "client_id";

    public OAuth2AccessTokenWithAdditionalInfo createToken(String tokenString) {
        Map<String, Object> tokenInfo = parseToken(tokenString);
        return createToken(tokenString, tokenInfo);
    }

    @SuppressWarnings("unchecked")
    public OAuth2AccessTokenWithAdditionalInfo createToken(String tokenString, Map<String, Object> tokenInfo) {
        List<String> scope = (List<String>) tokenInfo.get(SCOPE);
        Number expiresAt = (Number) tokenInfo.get(EXP);
        Number instantiatedAt = (Number) tokenInfo.get(IAT);
        if (scope == null || expiresAt == null || instantiatedAt == null) {
            return null;
        }
        return new OAuth2AccessTokenWithAdditionalInfo(createOAuth2AccessToken(tokenString, scope, expiresAt, instantiatedAt), tokenInfo);
    }

    private OAuth2AccessToken createOAuth2AccessToken(String tokenString, List<String> scope, Number expiresAt, Number instantiatedAt) {
        try {
            return new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                                         tokenString,
                                         new Date(instantiatedAt.longValue() * 1000).toInstant(),
                                         new Date(expiresAt.longValue() * 1000).toInstant(),
                                         new HashSet<>(scope));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage(), e);
        }
    }

    private Map<String, Object> parseToken(String tokenString) {
        String[] tokenParts = tokenString.split("\\.");
        if (tokenParts.length != 3) {
            // The token should have three parts (header, body and signature) separated by a dot. It doesn't, so we consider it as invalid.
            return Collections.emptyMap();
        }
        String body = decode(tokenParts[1]);
        return JsonUtil.convertJsonToMap(body);
    }

    private String decode(String string) {
        Decoder decoder = Base64.getUrlDecoder();
        return new String(decoder.decode(string), StandardCharsets.UTF_8);
    }

    public OAuth2AccessTokenWithAdditionalInfo createToken(Oauth2AccessTokenResponse oauth2AccessTokenResponse) {
        return createToken(oauth2AccessTokenResponse.getAccessToken());
    }

}
