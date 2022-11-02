package it.gov.pagopa.forwarder.service;

import it.gov.pagopa.forwarder.config.SslConfig;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Enumeration;


@Service
public class ProxyService {

    static private String HD_HOST_URL = "X-Host-Url";
    static private String HD_HOST_PORT = "X-Host-Port";
    static private String HD_HOST_PATH = "X-Host-Path";
    static private String X_REQUEST_ID = "X-Request-Id";

    @Value("${certificate.path}")
    private String certificatePath;

    @Value("${certificate.key}")
    private String certificateKey;

    private final static Logger logger = LogManager.getLogger(ProxyService.class);

    @Retryable(exclude = {
            HttpStatusCodeException.class}, include = Exception.class,
            backoff = @Backoff(delay = 5000, multiplier = 4.0), maxAttempts = 4)
    public ResponseEntity<String> processProxyRequest(String body,
                                                      HttpMethod method,
                                                      HttpServletRequest request,
                                                      HttpServletResponse response,
                                                      String traceId)
            throws URISyntaxException, UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, InvalidKeySpecException, KeyManagementException {
        ThreadContext.put("traceId", traceId);

        // Create headers configuration
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.set(headerName, request.getHeader(headerName));
//            logger.info("hd {} - {}", headerName, request.getHeader(headerName));
        }

        headers.set(X_REQUEST_ID, traceId);
        headers.remove(HttpHeaders.ACCEPT_ENCODING);

        // construct URI for the request
        String domain = request.getHeader(HD_HOST_URL);
        int port = Integer.parseInt(request.getHeader(HD_HOST_PORT));
        String requestUrl = request.getHeader(HD_HOST_PATH);
        requestUrl = requestUrl.startsWith("/") ? requestUrl : String.format("/%s", requestUrl);

        URI uri = new URI("https", null, domain , port, requestUrl, request.getQueryString(), null);

        // set client certificate in the request
        // retrieve client certificate
        String cert = FileUtils.readFileToString(new File(certificatePath), StandardCharsets.UTF_8);

        // SSL configuration
        SSLContext sslContext = SslConfig.getSSLContext(cert, certificateKey, null);
        HttpClient httpClient = HttpClients.custom().setSSLContext(sslContext).build();
        ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        RestTemplate restTemplate = new RestTemplate(requestFactory);

        HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);

        try {

            ResponseEntity<String> serverResponse = restTemplate.exchange(uri, method, httpEntity, String.class);
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.put(HttpHeaders.CONTENT_TYPE, serverResponse.getHeaders().get(HttpHeaders.CONTENT_TYPE));
            logger.info(serverResponse);
            return serverResponse;


        } catch (HttpStatusCodeException e) {
            logger.error("HTTP Status Code Exception", e);
            return ResponseEntity.status(e.getRawStatusCode())
                    .headers(e.getResponseHeaders())
                    .body(e.getResponseBodyAsString());
        }

    }

    @Recover
    public ResponseEntity<String> recoverFromRestClientErrors(Exception e, String body,
                                                              HttpMethod method, HttpServletRequest request, HttpServletResponse response, String traceId) {
        logger.error("retry method for the following url " + request.getRequestURI() + " has failed" + e.getMessage());
        logger.error(e.getStackTrace());
        throw new RuntimeException("There was an error trying to process you request. Please try again later");
    }

}
