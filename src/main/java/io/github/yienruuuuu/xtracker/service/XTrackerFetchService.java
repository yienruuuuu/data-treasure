package io.github.yienruuuuu.xtracker.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.yienruuuuu.common.error.SysCode;
import io.github.yienruuuuu.common.exception.InternalApiException;
import io.github.yienruuuuu.xtracker.bean.dto.XTrackerPostsFetchResult;
import io.github.yienruuuuu.xtracker.bean.dto.XTrackerSyncOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.time.Duration;

/**
 * Fetches XTracker API responses for crawler ingestion.
 */
@Slf4j
@Service
public class XTrackerFetchService {

    static final String DEFAULT_BASE_URL = "https://xtracker.polymarket.com/api";
    static final String POSTS_ENDPOINT_TEMPLATE = "/users/{handle}/posts";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final Duration connectTimeout;
    private final Duration readTimeout;

    public XTrackerFetchService(
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper,
            @Value("${xtracker.api.base-url:" + DEFAULT_BASE_URL + "}") String baseUrl,
            @Value("${xtracker.api.insecure-ssl:false}") boolean insecureSsl,
            @Value("${xtracker.api.connect-timeout:PT5S}") Duration connectTimeout,
            @Value("${xtracker.api.read-timeout:PT20S}") Duration readTimeout
    ) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.restTemplate = buildRestTemplate(restTemplateBuilder, insecureSsl);
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
    }

    public XTrackerPostsFetchResult fetchPosts(String platform, String handle, XTrackerSyncOptions options) {
        String normalizedHandle = normalizeHandle(handle);
        String endpoint = POSTS_ENDPOINT_TEMPLATE.replace("{handle}", normalizedHandle);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path(endpoint)
                .queryParam("platform", platform);
        if (options.startDate() != null) {
            builder.queryParam("startDate", options.startDate());
        }
        if (options.endDate() != null) {
            builder.queryParam("endDate", options.endDate());
        }
        if (options.timezone() != null && !options.timezone().isBlank()) {
            builder.queryParam("timezone", options.timezone().trim());
        }
        String url = builder.toUriString();

        log.debug("Fetching XTracker posts. platform={}, handle={}, url={}", platform, normalizedHandle, url);
        ResponseEntity<String> response;
        try {
            response = restTemplate.getForEntity(url, String.class);
        } catch (RestClientException exception) {
            throw new InternalApiException(
                    SysCode.INTERNAL_ERROR,
                    "Failed to fetch XTracker posts: " + normalizedHandle,
                    exception
            );
        }
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new InternalApiException(
                    SysCode.INTERNAL_ERROR,
                    "Failed to fetch XTracker posts: " + normalizedHandle,
                    null
            );
        }

        try {
            JsonNode body = objectMapper.readTree(response.getBody());
            log.debug("Fetched XTracker posts. platform={}, handle={}, status={}, bodyLength={}",
                    platform, normalizedHandle, response.getStatusCode().value(), response.getBody().length());
            return new XTrackerPostsFetchResult(
                    endpoint,
                    platform,
                    normalizedHandle,
                    options.sourceObjectId(platform, normalizedHandle),
                    options.toRequestParams(objectMapper, platform),
                    response.getStatusCode().value(),
                    response.getBody(),
                    body
            );
        } catch (JsonProcessingException exception) {
            throw new InternalApiException(
                    SysCode.INTERNAL_ERROR,
                    "XTracker posts API returned malformed JSON: " + normalizedHandle,
                    exception
            );
        }
    }

    private String normalizeHandle(String handle) {
        return handle == null ? "" : handle.trim().replaceFirst("^@", "");
    }

    private RestTemplate buildRestTemplate(RestTemplateBuilder restTemplateBuilder, boolean insecureSsl) {
        if (!insecureSsl) {
            return restTemplateBuilder
                    .setConnectTimeout(connectTimeout)
                    .setReadTimeout(readTimeout)
                    .build();
        }
        log.warn("XTracker API insecure SSL mode is enabled. Use only for local development or controlled testing.");
        InsecureSslRequestFactory requestFactory = new InsecureSslRequestFactory(noopHostnameVerifier());
        requestFactory.setConnectTimeout((int) connectTimeout.toMillis());
        requestFactory.setReadTimeout((int) readTimeout.toMillis());
        return restTemplateBuilder
                .requestFactory(() -> requestFactory)
                .build();
    }

    private HostnameVerifier noopHostnameVerifier() {
        return (hostname, session) -> true;
    }

    private static class InsecureSslRequestFactory extends SimpleClientHttpRequestFactory {

        private final javax.net.ssl.SSLSocketFactory sslSocketFactory;
        private final HostnameVerifier hostnameVerifier;

        private InsecureSslRequestFactory(HostnameVerifier hostnameVerifier) {
            this.sslSocketFactory = createTrustAllSslSocketFactory();
            this.hostnameVerifier = hostnameVerifier;
        }

        @Override
        protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
            if (connection instanceof HttpsURLConnection httpsConnection) {
                httpsConnection.setSSLSocketFactory(sslSocketFactory);
                httpsConnection.setHostnameVerifier(hostnameVerifier);
            }
            super.prepareConnection(connection, httpMethod);
        }

        private static javax.net.ssl.SSLSocketFactory createTrustAllSslSocketFactory() {
            try {
                TrustManager[] trustManagers = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                            }

                            @Override
                            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                            }

                            @Override
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }
                        }
                };
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustManagers, null);
                return sslContext.getSocketFactory();
            } catch (GeneralSecurityException exception) {
                throw new IllegalStateException("Failed to create insecure XTracker SSL context", exception);
            }
        }
    }
}
