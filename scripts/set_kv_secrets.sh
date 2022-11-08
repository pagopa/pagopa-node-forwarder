
az keyvault secret set --vault-name "pagopa-d-kv" --name "certificate-crt-node-forwarder" --file "certificate.crt"
az keyvault secret set --vault-name "pagopa-d-kv" --name "certificate-key-node-forwarder" --file "certificate-key.pem"
