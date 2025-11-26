#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# set_kv_secrets.sh
#
# Descrizione:
#   Script per caricare il certificato e la chiave privata (in formato PKCS#8)
#   nel Key Vault di Azure per l'ambiente specificato (dev|uat|prod).
#
# Funzionalità principali:
#   - Verifica che sia passato l'argomento ambiente e che i file necessari
#     esistano (.pem e chiave PKCS#1).
#   - Converte la chiave privata da PKCS#1 a PKCS#8 usando openssl.
#   - Estrae il file .crt dal file .pem.
#   - Carica i file risultanti come secret nel Key Vault Azure:
#       * certificate-crt-node-forwarder  -> file .crt
#       * certificate-key-node-forwarder  -> chiave privata in PKCS#8
#
# Requisiti:
#   - Azure CLI (az) autenticata e configurata per la subscription corretta
#   - openssl installato
#
# Uso:
#   sh scripts/set_kv_secrets.sh dev|uat|prod
#
# Effetti collaterali:
#   - Genera ../certs/<env>-certificate.crt
#   - Genera ../certs/<env>-private.key in formato PKCS#8 (sovrascrive il file se esiste)
#   - Carica i secret nel Key Vault relativo all'ambiente
#
# Note operative:
#   - Il nome del Key Vault è derivato come pagopa-<d|u|p>-kv (prima lettera dell'ambiente)
#   - Assicurarsi di avere i permessi per eseguire az keyvault secret set sul Key Vault
# ----------------

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
