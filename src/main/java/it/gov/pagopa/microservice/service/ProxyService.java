package it.gov.pagopa.microservice.service;

import it.gov.pagopa.microservice.config.SslConfig;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.net.ssl.SSLContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Enumeration;


@Service
public class ProxyService {

    static private String HD_HOST_URL = "X-Host-Url";
    static private String HD_HOST_PORT = "X-Host-Port";
    static private String HD_HOST_PATH = "X-Host-Path";

    private String cert = "-----BEGIN CERTIFICATE-----\n" +
            "MIIETzCCAzcCCQDT3UyIt5ur0jANBgkqhkiG9w0BAQUFADBmMQswCQYDVQQGEwJh\n" +
            "YTELMAkGA1UECAwCYWExCzAJBgNVBAcMAmFhMQswCQYDVQQKDAJhYTELMAkGA1UE\n" +
            "CwwCYWExEDAOBgNVBAMMB2JiYi5jb20xETAPBgkqhkiG9w0BCQEWAmFhMB4XDTIy\n" +
            "MDcxODE3MTcyMVoXDTIzMDcxODE3MTcyMVowbTELMAkGA1UEBhMCYWExCzAJBgNV\n" +
            "BAgMAmFhMQswCQYDVQQHDAJhYTELMAkGA1UECgwCYWExCzAJBgNVBAsMAmFhMRcw\n" +
            "FQYDVQQDDA5jbGllbnQuYmJiLmNvbTERMA8GCSqGSIb3DQEJARYCYWEwggIiMA0G\n" +
            "CSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQDJ2UYwEAMveMGlLqZhNl5iy+pZCAEM\n" +
            "qscHnoElYTX675qpW45dpzYwQEi5AX354A1tmLbsKPmprLNIdOieSFOyWQq78a0M\n" +
            "FE2Md8v7xfHZYBgvkTNxjZbpKmODTX8XpWmB9ajlZ6TVTz0FlehVfqPsrdCZN2f9\n" +
            "pzRYgEsIHkOA0K1tJKM+hfPDSGRp1G9Xco9t29Kq9SjAfU6SxNOAQuUll38loZkz\n" +
            "wUBccfj3GWuxFCTDfaVNzkAsxWonjjTSBgBsRrT+Zopqcoc52gPUliGTM+Z+VS/W\n" +
            "IOw4hNgCpRp5s2RE1cU1/U/CX+EMWlmULeYFAtxG1uK5ZAzn/oFR7McUifz0TByO\n" +
            "TF4Fto5FN6f/7+eIwVSj2QwO707iH42xzd7r+TBuvUH0X2Hm2gg+s8JVqnpniIwe\n" +
            "C0dL9NVIhUH8E6aKpLl+V3C33EFrPSpQ/qwdjqB76YzyaLl6YNx5K3L0torlujzu\n" +
            "gMJuZc/x9Ml83Lb0Ro9yJp3G8HidZVAIf+B0um5DyiWcGQEihVVMZMbixzHc1x+G\n" +
            "ZRk1IqR0sXUv7keN93qf3A1iXeK8MqQIfVA7KKMSVDZoMcfL6/PPC+0fY01sCZFo\n" +
            "y6x+vdNdQ23s0q6sX/wyybEU/IYEQf4e5+Owt5o6Mz0dmsJwth2jtGZhCLtKJFXs\n" +
            "aUbSPEIAUEHJSQIDAQABMA0GCSqGSIb3DQEBBQUAA4IBAQBXG3NhF7ii0boQmb8o\n" +
            "ZBJaCWBmyXeHSgROuRRl6xLidJNXkpaZSfKktcsrvLIgOKy6XtuQNaw+NbrANfAP\n" +
            "YIVBkoySllZTk4DKH0O8tjSdnrB2gjBENVACXPPIrJQsM1OKvedvBw4IUVKDSks6\n" +
            "bN9VMsDMKrja7imHkEnU0x5+YMsBlt4tnEho+AueFbzjT1m3PV0pFZR+B9yxz2Dg\n" +
            "s4pggNsusQNr/O6czEca3xjLKAR6fOVNgl2qLn20SZkL22jWSylfRiAvVLV9sgam\n" +
            "kx0eddcO/3RUvsSsaltBaxaG0LYTMo24uinZHK4SR6sTxCk49NWKxjmWY9Hz+cAQ\n" +
            "NscL\n" +
            "-----END CERTIFICATE-----\n";

    String domain = "server.aaa.com";
    int port = 8888;
//    String domain = "www.mocky.io/v2/5cb6b0c13200003c12cd453f";
//    int myPort = -1;

//    String domain = "127.0.0.1";
//    int myPort = 8089;

    private final static Logger logger = LogManager.getLogger(ProxyService.class);

    @Retryable(exclude = {
            HttpStatusCodeException.class}, include = Exception.class, backoff = @Backoff(delay = 5000, multiplier = 4.0), maxAttempts = 4)
    public ResponseEntity<String> processProxyRequest(String body,
                                                      HttpMethod method, HttpServletRequest request, HttpServletResponse response, String traceId) throws URISyntaxException, UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, InvalidKeySpecException, KeyManagementException {
        ThreadContext.put("traceId", traceId);
        String requestUrl = request.getRequestURI();
        requestUrl = "/";

        // Headers
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.set(headerName, request.getHeader(headerName));
            logger.info("hd {} - {}",headerName, request.getHeader(headerName));
        }

        headers.set("TRACE", traceId);
        headers.remove(HttpHeaders.ACCEPT_ENCODING);

        //log if required in this line
        domain = request.getHeader(HD_HOST_URL);
        port = Integer.parseInt(request.getHeader(HD_HOST_PORT));
        requestUrl = request.getHeader(HD_HOST_PATH);
        URI uri = new URI("https", null, domain , port, null, null, null);

        // replacing context path form urI to match actual gateway URI
        uri = UriComponentsBuilder.fromUri(uri)
                .path(requestUrl)
                .query(request.getQueryString())
                .build(true).toUri();


// --------------- start SSLconfig
        SSLContext sc = SslConfig.getSSLSocketFactory(cert, null, null);
        HttpClient httpClient = HttpClients.custom().setSSLContext(sc).build();
        ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
// --------------- stop SSLconfig

        HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);
        ClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
//        RestTemplate restTemplate = new RestTemplate(factory); // original`
        try {

            ResponseEntity<String> serverResponse = restTemplate.exchange(uri, method, httpEntity, String.class);
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.put(HttpHeaders.CONTENT_TYPE, serverResponse.getHeaders().get(HttpHeaders.CONTENT_TYPE));
            logger.info(serverResponse);
            return serverResponse;


        } catch (HttpStatusCodeException e) {
            logger.error(e.getMessage());
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
