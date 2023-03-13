locals {
  github = {
    org        = "pagopa"
    repository = "pagopa-node-forwarder"
  }

  azure = {
    product = "pagopa-${var.env_short}"
    project = "${local.azure.product}-${local.azure.domain.location_short}-${local.azure.domain.name}"

    domain = {
      name           = "apiconfig"
      location_short = "weu"
    }
  }
}
