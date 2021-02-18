package com.sap.cloudfoundry.client.facade.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.Map;

import javax.net.ssl.SSLException;
import javax.net.ssl.X509TrustManager;

import org.cloudfoundry.reactor.ConnectionContext;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;
import com.sap.cloudfoundry.client.facade.oauth2.OAuthClientWithLoginHint;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import reactor.netty.http.client.HttpClient;

/**
 * Some helper utilities for creating classes used for the REST support.
 *
 */
public class RestUtil {

    private static final int MAX_IN_MEMORY_SIZE = 1 * 1024 * 1024; // 1MB
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OAuthClient createOAuthClient(URL controllerUrl, ConnectionContext connectionContext, String origin,
                                         boolean shouldTrustSelfSignedCertificates) {
        Map<String, Object> infoMap = getInfoMap(controllerUrl, createWebClient(shouldTrustSelfSignedCertificates));
        URL authorizationUrl = getAuthorizationUrl(infoMap);
        return new OAuthClientWithLoginHint(authorizationUrl, connectionContext, origin, createWebClient(true));
    }

    public OAuthClient createOAuthClientByControllerUrl(URL controllerUrl, boolean shouldTrustSelfSignedCertificates) {
        WebClient webClient = createWebClient(shouldTrustSelfSignedCertificates);
        URL authorizationUrl = getAuthorizationUrl(controllerUrl, webClient);
        return new OAuthClient(authorizationUrl, webClient);
    }

    private URL getAuthorizationUrl(URL controllerUrl, WebClient webClient) {
        Map<String, Object> infoMap = getInfoMap(controllerUrl, webClient);
        return getAuthorizationUrl(infoMap);
    }

    // TODO: Refactor not to use v2
    private Map<String, Object> getInfoMap(URL controllerUrl, WebClient webClient) {
        String infoResponse = webClient.get()
                                       .uri(controllerUrl + "/v2/info")
                                       .retrieve()
                                       .bodyToMono(String.class)
                                       .block();
        try {
            return objectMapper.readValue(infoResponse, new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException e) {
            throw new IllegalArgumentException("Error getting /v2/info from cloud controller.", e);
        }
    }

    private URL getAuthorizationUrl(Map<String, Object> infoMap) {
        String authorizationEndpoint = (String) infoMap.get("authorization_endpoint");
        try {
            return new URL(authorizationEndpoint);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(MessageFormat.format("Error creating authorization endpoint URL for endpoint {0}.",
                                                                    authorizationEndpoint),
                                               e);
        }
    }

    public WebClient createWebClient(boolean trustSelfSignedCerts) {
        return WebClient.builder()
                        .exchangeStrategies(ExchangeStrategies.builder()
                                                              .codecs(configurer -> configurer.defaultCodecs()
                                                                                              .maxInMemorySize(MAX_IN_MEMORY_SIZE))
                                                              .build())
                        .clientConnector(buildClientConnector(trustSelfSignedCerts))
                        .build();
    }

    private ClientHttpConnector buildClientConnector(boolean trustSelfSignedCerts) {
        HttpClient httpClient = HttpClient.create()
                                          .followRedirect(true);
        if (trustSelfSignedCerts) {
            httpClient = httpClient.secure(sslContextSpec -> sslContextSpec.sslContext(buildSslContext()));
        } else {
            httpClient = httpClient.secure();
        }
        return new ReactorClientHttpConnector(httpClient);
    }

    private SslContext buildSslContext() {
        try {
            return SslContextBuilder.forClient()
                                    .trustManager(createDummyTrustManager())
                                    .build();
        } catch (SSLException e) {
            throw new RuntimeException("An error occurred setting up the SSLContext", e);
        }
    }

    private X509TrustManager createDummyTrustManager() {
        return new X509TrustManager() {

            @Override
            public void checkClientTrusted(X509Certificate[] xcs, String string) {
                // NOSONAR
            }

            @Override
            public void checkServerTrusted(X509Certificate[] xcs, String string) {
                // NOSONAR
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[] {};
            }

        };
    }
}
