#!/usr/bin/env bash

if [ $# -eq 0 ]
  then
    echo "> sh set_kv_secrets.sh <dev|uat|prod>"
    exit
fi

environment=$1

pem_certificate="../certs/"
crt_certificate="../certs/$environment-certificate.crt"
pkcs1_private_key=""
pkcs8_private_key="../certs/"
kv_name="pagopa-${environment:0:1}-kv"

if [[ "$environment" == "dev" ]]; then
  pem_certificate+="forwarder.$environment.platform.pagopa.it.pem"
elif [[ "$environment" == "uat" ]]; then
  pem_certificate+="forwarder.$environment.platform.pagopa.it.pem"
elif [[ "$environment" == "prod" ]]; then
  pem_certificate+="forwarder.platform.pagopa.it.pem"
else
  echo "PEM Certificate not found"
  exit
fi

pkcs1_private_key="$environment-private.key"

if [[ ! -f "$pem_certificate" ]]; then
    echo "$pem_certificate not exist."
fi

if [[ ! -f "$pkcs1_private_key" ]]; then
    echo "$pkcs1_private_key not exist."
fi

pkcs8_private_key+="$pkcs1_private_key"

# convert private key PKCS1 to PKCS8
openssl pkcs8 -topk8 -nocrypt -in "$pkcs1_private_key" -out "$pkcs8_private_key"

# extract crt from pem
openssl x509 -outform pem -in "$pem_certificate" -out "$crt_certificate"

echo "uploading info into azure kv"
az keyvault secret set --vault-name "$kv_name" --name "certificate-crt-node-forwarder" --file "$crt_certificate"
az keyvault secret set --vault-name "$kv_name" --name "certificate-key-node-forwarder" --file "$pkcs8_private_key"
