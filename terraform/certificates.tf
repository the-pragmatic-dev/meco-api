resource "digitalocean_certificate" "cert" {
  name    = "meco"
  type    = "lets_encrypt"
  domains = [var.domain]

  depends_on = [
    digitalocean_domain.meco
  ]
}