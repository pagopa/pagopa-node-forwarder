
### How-To Certificati [Nuova Connettività]
- see [here](https://pagopa.atlassian.net/wiki/x/HIBZIw) to details
- [SANP ref](https://developer.pagopa.it/pago-pa/guides/sanp/3.9.1/appendici/connettivita)


### Steps(script) to update certificates
1. exec `get_check_kv_secrets.sh` to verify current certs status (pass env ex. `DEV`, `UAT`, `PROD`)
1. copy into `scripts` folder private key from [here](https://pagopa.atlassian.net/wiki/x/o4MbIw) and rename it to `<ENV>-private.key`
1. exec `set_kv_secrets.sh` to update certs in KeyVault (pass env ex. `DEV`, `UAT`, `PROD`)
1. apply new cert to node-forwarder app service (follow [here](https://github.com/pagopa/pagopa-infra/blob/main/src/next-core/06_node_forwarder.tf))
1. re-type step 1 to verify new certs status applied ✅


<details>
  <summary>TF apply command cli</summary>

```bash
./terraform.sh apply <ENV> -target="azurerm_resource_group.node_forwarder_rg" -target="module.node_forwarder_snet" -target="module.node_forwarder_ha_snet" -target="azurerm_subnet_nat_gateway_association.nodefw_ha_snet_nat_association" -target="module.node_forwarder_app_service" -target="module.node_forwarder_slot_staging" -target="azurerm_private_endpoint.forwarder_input_private_endpoint" -target="azurerm_private_endpoint.forwarder_staging_input_private_endpoint" -target="azurerm_monitor_autoscale_setting.node_forwarder_app_service_autoscale" -target="azurerm_monitor_metric_alert.app_service_over_cpu_usage" -target="azurerm_monitor_metric_alert.app_service_over_mem_usage" -target="azurerm_key_vault_secret.certificate_crt_node_forwarder_s" -target="azurerm_key_vault_secret.certificate_key_node_forwarder_s" -target="azurerm_key_vault_secret.node_forwarder_subscription_key" -target="azurerm_monitor_scheduled_query_rules_alert.opex_pagopa-node-forwarder-responsetime-upd" -target="azurerm_monitor_scheduled_query_rules_alert.opex_pagopa-node-forwarder-availability-upd"
```
</details>


