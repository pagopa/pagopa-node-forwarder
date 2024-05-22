#!/usr/bin/env bash

set -e

# https://knowledge.digicert.com/solution/how-certificate-chains-work

# This script is used for creating self-signed certificates for the development environment. It generates three distinct certificates:
# rootCA.pem --> Certificate of the Certification Authority
# intermediateCA.pem --> Certificate of the Intermediate Certification Authority
# final.pem --> Final certificate for the domain: `*.forwarder.dev.platform.pagopa.it`

# certificate expiry
rootCA_cert_expiry=1095
intermediateCA_cert_expiry=730
final_cert_expiry=365

# Generate rootCA private key
openssl genrsa -out rootCA/rootCA.key 2048

# Generate rootCA certificate
openssl req -x509 -days $rootCA_cert_expiry -new -key rootCA/rootCA.key -sha256 -out rootCA/rootCA.pem -config rootCA/rootCA.conf

# Generate intermediateCA private key
openssl genrsa -out intermediateCA/intermediateCA.key 2048

# Generate certificate signing request (CSR) intermediateCA
openssl req -new -key intermediateCA/intermediateCA.key -out intermediateCA/intermediateCA.csr --config intermediateCA/intermediateCA.csr.conf

# Create and sign the Intermediate Certificate using the Root CA.
openssl x509 -req -days $intermediateCA_cert_expiry -sha256 -in intermediateCA/intermediateCA.csr -CA rootCA/rootCA.pem -CAkey rootCA/rootCA.key -CAcreateserial -out intermediateCA/intermediateCA.pem

# Generate final private key
openssl genrsa -out final/final.key 2048

# Generate certificate signing request (CSR) final
openssl req -new -key final/final.key -out final/final.csr -config final/final.csr.conf

# Create and sign the Final Certificate using the Intermediate CA.
openssl x509 -req -days $final_cert_expiry -sha256 -in final/final.csr -CA intermediateCA/intermediateCA.pem -CAkey intermediateCA/intermediateCA.key -CAcreateserial -out final/final.pem

# Combine the certificates into a single file certs/forwarder.dev.platform.pagopa.it.pem
cat final/final.pem intermediateCA/intermediateCA.pem rootCA/rootCA.pem > ../../certs/forwarder.dev.platform.pagopa.it.pem
