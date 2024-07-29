package it.gov.pagopa.forwarder.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import it.gov.pagopa.forwarder.service.ProxyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.UUID;

@RestController
@Tag(name = "Proxy")
public class ProxyController {
    @Autowired
    ProxyService service;
    @PostMapping(value = "/forward")
    public ResponseEntity<String> forward(
            @RequestHeader(value="X-Host-Url") @NotNull String xHostUrl,
            @RequestHeader(value="X-Host-Port") @NotNull Integer xHostPort,
            @RequestHeader(value="X-Host-Path") @NotNull String xHostPath,
            @RequestBody(required = false) byte[] body,
                                                   HttpMethod method, HttpServletRequest request, HttpServletResponse response)
            throws URISyntaxException, UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, InvalidKeySpecException, KeyManagementException {
        String xRequestId = request.getHeader("X-Request-Id");
        return service.processProxyRequest(
                xHostUrl,
                xHostPort,
                xHostPath,
                body,
                method,
                request,
                response,
                xRequestId == null ? UUID.randomUUID().toString() : xRequestId);
    }
}
