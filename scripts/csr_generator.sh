#!/usr/bin/env bash

# useful info https://gist.github.com/yidas/af42d2952d85c0951c1722fcd68716c6

ENVIRONMENT=$1

if [ -z "$ENVIRONMENT" ]
then
  echo "No env specified: sh csr_generator.sh <dev|uat|prod>"
  exit 1
fi

configuration="csr.${ENVIRONMENT}.conf"
private_key="${ENVIRONMENT}-private.key"
csr="${ENVIRONMENT}-certificate.csr"

rm "${private_key}" "${csr}" 2> /dev/null

# generate an RSA private key
openssl genrsa -out "${private_key}"

# generate a certificate signing request (CSR) according to input configuration
openssl req -new -key "${private_key}" -out "${csr}" -config "${configuration}"

# self-sign certificate for dev environment
if [[ "$ENVIRONMENT" == "dev" ]]; then
  echo -n "Generating cert for dev environment"
  crt="dev-certificate.crt"
  openssl x509 -req -days 365 -in "$csr" -signkey "$private_key" -out "$crt" -extensions req_ext -extfile csr.dev.conf
  openssl x509 -in "$crt" -out "../certs/forwarder.dev.platform.pagopa.it.pem" -outform PEM
fi
