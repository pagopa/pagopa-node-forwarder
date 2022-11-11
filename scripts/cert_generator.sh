#!/usr/bin/env bash

# script to extract certificate and DER private key

if [ $# -eq 0 ]
  then
    echo "> sh cert_generator.sh <pagopa-certificate.pfx>"
    exit
fi

pfx_file=$1

# extract certificate
openssl pkcs12 -in "${pfx_file}" -clcerts -password pass: -nokeys -out certificate.crt

# extract encrypted private key
openssl pkcs12 -in "${pfx_file}" -password pass: -nocerts -nodes -out certificate-key.pem

# clean files
sed -i '' -e '/-----BEGIN/,$!d' certificate.crt
sed -i '' -e '/-----BEGIN/,$!d' certificate-key.pem

# convert PEM key to DER format (it is useful only for debug reading file directly)
# openssl pkcs8 -topk8 -inform PEM -outform DER -in certificate-key.pem -out certificate-key.der -nocrypt
