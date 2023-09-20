#!/usr/bin/env bash

# to check chain certificate
# openssl crl2pkcs7 -nocrl -certfile forwarder.platform.pagopa.it.pem | openssl pkcs7 -print_certs -noout

if [ $# -eq 0 ]
  then
    echo "> sh $0.sh <dev|uat|prod>"
    exit
fi

environment=$1
pem_certificate=""

kv_name="pagopa-${environment:0:1}-kv"

if [[ "$environment" == "dev" ]]; then
  pem_certificate+="forwarder.$environment.platform.pagopa.it.pem"
  shortenv="d"
  subscriptionid="bbe47ad4-08b3-4925-94c5-1278e5819b86"
elif [[ "$environment" == "uat" ]]; then
  pem_certificate+="forwarder.$environment.platform.pagopa.it.pem"
  shortenv="u"
  subscriptionid="26abc801-0d8f-4a6e-ac5f-8e81bcc09112"
elif [[ "$environment" == "prod" ]]; then
  pem_certificate+="forwarder.platform.pagopa.it.pem"
  shortenv="p"
  subscriptionid="PROD-pagoPA"
else
  echo "PEM Certificate not found"
  exit
fi

pkcs1_private_key="$environment-private.key"


echo "get and check $pem_certificate"
#az keyvault secret show --vault-name "$kv_name" --name "certificate-crt-node-forwarder"
az keyvault secret download --vault-name "$kv_name" --name "certificate-crt-node-forwarder" --file "current-$pem_certificate"
cert_on_kv=`openssl x509 -in "current-$pem_certificate" -text | grep -e "Subject: CN" -e "Not " -e "Serial Number:" -A 1`
#echo $cert_on_kv

cert_on_repo=`openssl x509 -in "../certs/$pem_certificate" -text | grep -e "Subject: CN" -e "Not " -e "Serial Number:" -A 1`
#echo $cert_on_repo

#echo "get cert on app"
#az account set -s "${SUBSCRIPTION}"
cert_on_app=`az webapp config appsettings list --name "pagopa-$shortenv-app-node-forwarder" --resource-group "pagopa-$shortenv-node-forwarder-rg" --subscription "$subscriptionid" | jq -r '.[] | select(.name | startswith("CERTIFICATE_CRT"))'.value | openssl x509 -text | grep -e "Subject: CN" -e "Not " -e "Serial Number:" -A 1`


if [ "$cert_on_kv" = "$cert_on_repo" ] && [ "$cert_on_repo" = "$cert_on_app" ]; then
    echo "Certs are equal"
    echo $cert_on_repo
else
    echo "Certs are not equal."
    echo "cert_on_kv\n $cert_on_kv"
    echo "cert_on_repo\n $cert_on_repo"
    echo "cert_on_app\n $cert_on_app"
fi







