variable "do_token" {}
variable "pub_key" {}
variable "pvt_key" {}
variable "ssh_fingerprint" {}

variable "do_region" {
  default = "lon1"
}

variable "do_image" {
  default = "centos-8-x64"
}

variable "do_size" {
  default = "s-1vcpu-1gb"
}

variable "domain" {
  default = "meco.dev"
}
