
### Convert PEM certificates and keys to DER

Convert client key from PEM to DER format typing :

`openssl pkcs8 -topk8 -inform PEM -outform DER -in certs/client-key.pem -out certs/client-key2.der -nocrypt`

to avoid [Read RSA Private Key Java](https://www.sneppets.com/java/util/private-key-algid-parse-error-not-a-sequence/)

### List entry in keystore via `keytool`
see [here](https://docs.oracle.com/en/java/javase/11/tools/keytool.html)

Open terminal and typing ( to list all entry ) :

`keytool -list -keystore $JAVA_HOME/lib/security/cacerts`

or , typing

`keytool -list -keystore $JAVA_HOME/lib/security/cacerts -alias <CERT_ALIAS>`

to list a single cert alias

### Import security certificate to the JRE Keystore 
see [here](https://www.ibm.com/docs/en/tnpm/1.4.2?topic=security-import-certificate-jre-keystore)

verify local `keystore` pointing out to `$JAVA_HOME/lib/security/cacerts` then typing : 

`keytool -import -trustcacerts -alias <CERT_ALIAS> -file C:\temp\mdeCert.cer -keystore cacerts`

for example:
`sudo keytool -import -trustcacerts -alias mockServerAAA -file certs/server-crt.pem -keystore $JAVA_HOME/lib/security/cacerts`
Note: use `changeit` as keystore password (default keystore password).

### How to manually ✋ test it

Open terminal and typing :

```sh
curl --location --request POST 'http://localhost:8080/forward' \
--header 'X-Host-Url: server.aaa.com' \
--header 'X-Host-Port: 8888' \
--header 'X-Host-Path: /path' \
--header 'Content-Type: application/xml' \
--data-raw '<ciao></ciao>'
```

### How to use DEV certificate
Read the section [here](https://pagopa.atlassian.net/wiki/spaces/PAG/pages/561021241/Connettivit+in+uscita#Gestione-certificati)
