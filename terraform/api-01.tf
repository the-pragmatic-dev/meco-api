resource "digitalocean_droplet" "api-01" {
  image              = var.do_image
  name               = "api-01"
  region             = var.do_region
  size               = var.do_size
  backups            = false
  monitoring         = true
  ipv6               = true
  private_networking = true
  tags               = [digitalocean_tag.api.id]

  ssh_keys = [
    var.ssh_fingerprint,
  ]

  connection {
      user        = "root"
      type        = "ssh"
      host        = self.ipv4_address
      private_key = file(var.pvt_key)
      timeout     = "2m"
    }

  provisioner "remote-exec" {
    inline = [
      "echo TODO",
    ]
  }
}
