
resource "digitalocean_firewall" "api-firewall" {
  name = "api-firewall"
  tags = [digitalocean_tag.api.id]

  inbound_rule {
    protocol                  = "icmp"
    source_load_balancer_uids = [digitalocean_loadbalancer.public.id]
  }

  inbound_rule {
    protocol                  = "tcp"
    port_range                = "22"
    source_load_balancer_uids = [digitalocean_loadbalancer.public.id]
  }

  inbound_rule {
    protocol                  = "tcp"
    port_range                = "80"
    source_load_balancer_uids = [digitalocean_loadbalancer.public.id]
  }

  outbound_rule {
    protocol                       = "icmp"
    destination_load_balancer_uids = [digitalocean_loadbalancer.public.id]
  }

  outbound_rule {
    protocol                       = "tcp"
    port_range                     = "22"
    destination_load_balancer_uids = [digitalocean_loadbalancer.public.id]
  }

  outbound_rule {
    protocol                       = "tcp"
    port_range                     = "80"
    destination_load_balancer_uids = [digitalocean_loadbalancer.public.id]
  }
}