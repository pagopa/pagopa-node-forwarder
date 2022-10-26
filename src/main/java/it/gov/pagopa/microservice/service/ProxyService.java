package it.gov.pagopa.microservice.service;

import it.gov.pagopa.microservice.config.SslConfig;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import static org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

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
import javax.net.ssl.SSLSocketFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Enumeration;

@Service
public class ProxyService {
//    private String cert;
//    private String key;
    private String certPassword = null;

    private String cert = "-----BEGIN CERTIFICATE-----\n"
            + "MIIDbjCCAlYCCQCqCOJPdeVj6DANBgkqhkiG9w0BAQsFADB5MQswCQYDVQQGEwJJ\n"
            + "VDENMAsGA1UECAwEUm9tYTENMAsGA1UEBwwEUm9tYTEPMA0GA1UECgwGUGFwYXBh\n"
            + "MQwwCgYDVQQLDANkZXYxEDAOBgNVBAMMB3Rlc3QuaXQxGzAZBgkqhkiG9w0BCQEW\n"
            + "DHRlc3RAdGVzdC5pdDAeFw0yMTA3MTUxMDM3NTBaFw0yMjA3MTUxMDM3NTBaMHkx\n"
            + "CzAJBgNVBAYTAklUMQ0wCwYDVQQIDARSb21hMQ0wCwYDVQQHDARSb21hMQ8wDQYD\n"
            + "VQQKDAZQYXBhcGExDDAKBgNVBAsMA2RldjEQMA4GA1UEAwwHdGVzdC5pdDEbMBkG\n"
            + "CSqGSIb3DQEJARYMdGVzdEB0ZXN0Lml0MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A\n"
            + "MIIBCgKCAQEAxvXkTvINAzjm7r+844AeB/GkP3JYQZkF21Gi6H21eZez2yvIawXu\n"
            + "txVGPDBetE2pGOSofvKOe+4pMN5hIyO41s/tkEOgaYofIOF/lACxsZA7xMEUB5Wz\n"
            + "USj9DUQXUSc/G6sePZK7mNQWN4sYwUVWe9BtHv9nQBHrkssZcbVANeJ8GR+l9CBB\n"
            + "C3ZMi1CFfvOXvwjaQrQDDOU9QtSsuvbidZOlqqBzaD786viJV3phOeY7bbRGYtT8\n"
            + "s3qP31V/IqMa1m+CsAzWD2xUD3yNlVhADoXNbOQghOqceD/7cV4hYK1UCU3qE2ID\n"
            + "ZYykY9DNX39ERqbi6Wgn87Y03hcBDRNi1QIDAQABMA0GCSqGSIb3DQEBCwUAA4IB\n"
            + "AQBHWKXl8YkqEhXaGDr/ZO5GeCnll+4UsXke2Cr/+CjCxVdPUbGs/Q0hDDy7qJ3j\n"
            + "wynAeEInDJJw9iDZEuhrP3KGlObv6zdAYkhD7Uf9vkUn1wnJ9Hd67hkis5I2iNJb\n"
            + "FXi25bJ/m2iJj3QARnOdV0mv014P8Pvb63t/S77Go7M0hPpJg7HFdTvU0JrE0h5u\n"
            + "H1TGOFO+6wfK5G+oIOpEJWainMX8+IAUHRG2F0Ym4pTbQbrAPHzxBIleJ8hQAIU/\n"
            + "Ml1DQnRI5lsSnC01TO7TIuqYZCutqdoOYp9Wx+Jf+mNIzpzWLEK+/mX9nhAIt+eB\n" + "Z6Sd7YsDC0eqekaXGs0s4ojs\n"
            + "-----END CERTIFICATE-----";

    private String key = "-----BEGIN PRIVATE KEY-----\n"
            + "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDG9eRO8g0DOObu\n"
            + "v7zjgB4H8aQ/clhBmQXbUaLofbV5l7PbK8hrBe63FUY8MF60TakY5Kh+8o577ikw\n"
            + "3mEjI7jWz+2QQ6Bpih8g4X+UALGxkDvEwRQHlbNRKP0NRBdRJz8bqx49kruY1BY3\n"
            + "ixjBRVZ70G0e/2dAEeuSyxlxtUA14nwZH6X0IEELdkyLUIV+85e/CNpCtAMM5T1C\n"
            + "1Ky69uJ1k6WqoHNoPvzq+IlXemE55jtttEZi1Pyzeo/fVX8ioxrWb4KwDNYPbFQP\n"
            + "fI2VWEAOhc1s5CCE6px4P/txXiFgrVQJTeoTYgNljKRj0M1ff0RGpuLpaCfztjTe\n"
            + "FwENE2LVAgMBAAECggEAGZhigaFz+Rkl5Er4UtAVjPISLjNYlT/JWBide4lIglb4\n"
            + "xVZRlysRNa5f9bhHRqzC9zfbUVGE8P6HgAjruCiNS90985Qvm8QwEvvPfMvGEFC8\n"
            + "z6gDGqKwid1bCIzc7wy8eqO95S/uQE/wd77GNX7lDFKY5yb2MnDbvGuyX0Vw+D1r\n"
            + "cBMinrXh56ptdqRNNcmc7LxgmhX8FR0QpwzWSXPwC7C+8YaA5/n7JjvKzbXkDjss\n"
            + "dq9RMXWLxj71kccNuos6u+p13mQIHjqOxEpzBtc6uqHciU7m9E/YGFiqoXMS8q2P\n"
            + "zcNIoSyl2ZrRWhbkrLv9f1TLJnZElX4UJqkvy0+UwQKBgQD1zl6hJNv52l/ZlRfg\n"
            + "OawaHFIk/k80EFMdEl1TO9UUUzYcZejv+h1A4pKXv9Y3oLTR04vfplULnw2dA/Ra\n"
            + "T9hf3XpVY6B6mL4T2GK0z3bB925LZRUf/kQcKnDokaB/SDAssC9CKNEg+nKkQwqq\n"
            + "a6nhnDRvJUwe8XsnmyjF8sg0xQKBgQDPNi4YlNSzmvzZIo9eu2au93svIn1WcCHY\n"
            + "pd6T0TrXKebqJvJJBoCeXEvMkC5TjtT2hsME6O4v0YLYyDssXa3BKZCWTV4Z3oFn\n"
            + "NRcifP/HVSsl/d3ARNuBLcBolqIFJqdI1HTJfC8UsZY4swAMT+hi5iJWRlQdyPli\n"
            + "j+x8fe/20QKBgDdzFmXDqtvyJy0uNPSgDfLV8LHnrHZSOG2WdvcPyEGNg+dkFegM\n"
            + "xAyfD/Krk+d4mVG8JxGMtCKq/qcs/TJiUK1PiQk6MDl3u2k+pwWix2d6KadYZiuR\n"
            + "cEvqRw4vy8Tqw/NQy1hhXMvTs4jals1a/DeoxStNfp58WwvUNJ5y5jcBAoGBAL+4\n"
            + "48G+w2dDOjw16+28+29ccM/V256Etexc3KpsZ0L59DwmuPq0V4Eu6LtnlFWfzJAl\n"
            + "dIAwfWIlOioWahnMu54ENVG8WBsbcyPpTXSNr6Phu8C1Od5SV78Yc/TRmyuk7hdG\n"
            + "7KAYlP9SqSnhBWRe8ye+w3qMK/w7HfQCMs+lPshhAoGATJGoitcEaffjoJMipNtx\n"
            + "9QzPIf9jfj7H5a5+frmjdYEOH4EcQ0az0P2apj0twUD3Yu2BOc4YCnfzEOr/5Hz9\n"
            + "t5OKJ2UEwwNzzqRaz9E7gnONyT09pb0rI3u2IYwzZt/uQKLGn+f6C3t77J37kINo\n" + "gN4c44m6wDv/oFdcUkESrUQ=\n"
            + "-----END PRIVATE KEY-----";


    String domain = "server.aaa.com";
    int myPort = 8888;
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

        //log if required in this line
        URI uri = new URI("https", null, domain, myPort, null, null, null);

        // replacing context path form urI to match actual gateway URI
        uri = UriComponentsBuilder.fromUri(uri)
//                .path(requestUrl)
                .path("/")
                .query(request.getQueryString())
                .build(true).toUri();

        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.set(headerName, request.getHeader(headerName));
        }

        headers.set("TRACE", traceId);
        headers.remove(HttpHeaders.ACCEPT_ENCODING);


// --------------- start SSLconfig
//        SSLSocketFactory sf = SslConfig.getSSLSocketFactory(cert, key, certPassword);
//        SSLContext sc = SslConfig.getSSLSocketFactory2(cert, key, certPassword);
//        HttpClient httpClient = HttpClients.custom().setSSLContext(sc).build();
//        ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
//        RestTemplate restTemplate = new RestTemplate(requestFactory);
// --------------- stop SSLconfig

        HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);
        ClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory); // original`
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


    public void initSslConfiguration() throws UnrecoverableKeyException, CertificateException, IOException,
            NoSuchAlgorithmException, KeyStoreException, InvalidKeySpecException, KeyManagementException {

            SslConfig.getSSLSocketFactory(cert, key, certPassword);

    }
}
