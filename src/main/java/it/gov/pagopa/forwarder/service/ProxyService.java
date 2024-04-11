package it.gov.pagopa.forwarder.service;

import it.gov.pagopa.forwarder.config.SslConfig;
import it.gov.pagopa.forwarder.exception.AppException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;


@Service
public class ProxyService {
    private static final String X_REQUEST_ID = "X-Request-Id";
    private static final String OCP_APIM_SUBSCRIPTION_KEY = "Ocp-Apim-Subscription-Key";

    @Value("${certificate.crt}")
    private String certificate;

    @Value("${certificate.key}")
    private String certificateKey;

    @Value("${info.application.version}")
    private String nodeForwarderVersion;

    @Value("${pool.max-connection}")
    private Integer maxConnection;

    @Value("${pool.max-connection.per-route}")
    private Integer maxConnectionPerRoute;

    @Value("${pool.timeout}")
    private Integer connTimeout;

    private RestTemplate restTemplate;

    private static final Logger logger = LogManager.getLogger(ProxyService.class);

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
        //headers.remove(HttpHeaders.ACCEPT_ENCODING);
        headers.remove(OCP_APIM_SUBSCRIPTION_KEY); // remove subkey's header

        // construct URI for the request
        xHostPath = xHostPath.startsWith("/") ? xHostPath : String.format("/%s", xHostPath);
        URI uri = new URI("https", null, xHostUrl, xHostPort, xHostPath, request.getQueryString(), null);

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
            logger.info("Node Forwarder version: {}", nodeForwarderVersion);
            logger.info("https req {} >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> {} body {}\n", method, uri, httpEntity);

            ResponseEntity<String> serverResponse = restTemplate.exchange(uri, method, httpEntity, String.class);
            logger.info("server resp {}", serverResponse);
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
        } catch (Exception e) {
            logger.error( "Exception", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Forwarder Generic Error: " + e.getMessage());
        }


    }

    private void setRestTemplate() throws IOException, UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, InvalidKeySpecException, KeyManagementException {
        // set client certificate in the request
        // SSL configuration
        SSLContext sslContext = SslConfig.getSSLContext(certificate, certificateKey, null);

        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);

        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
                .<ConnectionSocketFactory>create()
                .register("https", socketFactory)
                .build();
        PoolingHttpClientConnectionManager poolingConnManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        poolingConnManager.setMaxTotal(maxConnection); // default 20
        poolingConnManager.setDefaultMaxPerRoute(maxConnectionPerRoute); // default 2

        // λ = L / W  => RPS = parallel connections / Request Time
        // 200 rps = x / 0.1 s => x = 20
        int timeout = connTimeout;
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000).build();

        HttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(socketFactory)
                .setConnectionManager(poolingConnManager)
                .setDefaultRequestConfig(config)
                .build();

        ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        RestTemplate restTemp = new RestTemplate(requestFactory);


        List<ClientHttpRequestInterceptor> interceptors = restTemp.getInterceptors();
        if (CollectionUtils.isEmpty(interceptors)) {
            interceptors = new ArrayList<>();
        }
        interceptors.add(new RestTemplateHeaderModifierInterceptor());
        restTemp.setInterceptors(interceptors);

        this.restTemplate = restTemp;



    }

    @Recover
    public ResponseEntity<String> recoverFromRestClientErrors(Exception e, String body,
                                                              HttpMethod method, HttpServletRequest request, HttpServletResponse response, String traceId) {
        logger.error("retry method for the following url {} has failed {}", request.getRequestURI(), e.getMessage());
        logger.error(e.getStackTrace());
        throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong", "There was an error trying to process you request. Please try again later");
    }

}
