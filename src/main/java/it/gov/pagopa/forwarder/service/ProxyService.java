package it.gov.pagopa.forwarder.service;

import it.gov.pagopa.forwarder.config.SslConfig;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Enumeration;


@Service
public class ProxyService {
    static private String X_REQUEST_ID = "X-Request-Id";

    @Value("${certificate.crt}")
    private String certificate;

    @Value("${certificate.key}")
    private String certificateKey;

    private RestTemplate restTemplate;

    private final static Logger logger = LogManager.getLogger(ProxyService.class);

    @Retryable(exclude = {
            HttpStatusCodeException.class}, include = Exception.class,
            backoff = @Backoff(delay = 5000, multiplier = 4.0), maxAttempts = 4)
    public ResponseEntity<String> processProxyRequest(
            String xHostUrl, Integer xHostPort, String xHostPath, String body,
            HttpMethod method, HttpServletRequest request, HttpServletResponse response, String xRequestId)
            throws URISyntaxException, UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, InvalidKeySpecException, KeyManagementException {

        // Create headers configuration
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.set(headerName, request.getHeader(headerName));
        }

        headers.set(X_REQUEST_ID, xRequestId);
        headers.set(HttpHeaders.HOST, xHostUrl);
        headers.remove(HttpHeaders.ACCEPT_ENCODING);

        // construct URI for the request
        xHostPath = xHostPath.startsWith("/") ? xHostPath : String.format("/%s", xHostPath);
        URI uri = new URI("https", null, xHostUrl , xHostPort, xHostPath, request.getQueryString(), null);

        // set client certificate in the request
        if (this.restTemplate == null) {
            this.setRestTemplate();
        }

        HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);

//        // --- path to disable manually mTSL - START
//        ClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
//        restTemplate = new RestTemplate(factory); // original`
//        // --- path to disable manually mTSL - STOP

        try {
            logger.info("https req {} >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> {} body {}\n", method, uri, httpEntity);

            ResponseEntity<String> serverResponse = restTemplate.exchange(uri, method, httpEntity, String.class);
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.put(HttpHeaders.CONTENT_TYPE, serverResponse.getHeaders().get(HttpHeaders.CONTENT_TYPE));
            logger.info("server resp {}",serverResponse);
            return serverResponse;

        } catch (HttpStatusCodeException e) {
            logger.error("HTTP Status Code Exception", e);
            return ResponseEntity.status(e.getRawStatusCode())
                    .headers(e.getResponseHeaders())
                    .body(e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            logger.error("HTTP Status Code Exception", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("mTLS failed versus CI/PSP. Error: " + e.getMessage());
        }

    }

    private void setRestTemplate() throws IOException, UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, InvalidKeySpecException, KeyManagementException {
        // set client certificate in the request
        // SSL configuration
        SSLContext sslContext = SslConfig.getSSLContext(certificate, certificateKey, null);
        HttpClient httpClient = HttpClients.custom().setSSLContext(sslContext).build();
        ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    @Recover
    public ResponseEntity<String> recoverFromRestClientErrors(Exception e, String body,
                                                              HttpMethod method, HttpServletRequest request, HttpServletResponse response, String traceId) {
        logger.error("retry method for the following url " + request.getRequestURI() + " has failed" + e.getMessage());
        logger.error(e.getStackTrace());
        throw new RuntimeException("There was an error trying to process you request. Please try again later");
    }

}
