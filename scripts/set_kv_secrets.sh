#!/usr/bin/env bash

if [ $# -eq 0 ]
  then
    echo "> sh set_kv_secrets.sh <d|u|p>"
    exit
fi

environment=$1

az keyvault secret set --vault-name "pagopa-${environment}-kv" --name "certificate-crt-node-forwarder" --file "certificate.crt"
az keyvault secret set --vault-name "pagopa-${environment}-kv" --name "certificate-key-node-forwarder" --file "certificate-key.pem"
