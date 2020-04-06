resource "digitalocean_loadbalancer" "public" {
  name                   = "loadbalancer-api"
  region                 = "lon1"
  algorithm              = "round_robin"
  redirect_http_to_https = true
  droplet_tag            = digitalocean_tag.api.name

  forwarding_rule {
    certificate_id  = digitalocean_certificate.cert.id
    tls_passthrough = false

    entry_port     = 443
    entry_protocol = "https"

    target_port     = 80
    target_protocol = "http"
  }

  healthcheck {
    port     = 22
    protocol = "tcp"
  }
}