resource "digitalocean_database_cluster" "postgres-cluster" {
  name       = "postgres-cluster"
  engine     = "pg"
  version    = "11"
  size       = "db-s-1vcpu-1gb"
  region     = "lon1"
  node_count = 1
  tags       = [digitalocean_tag.database.id]
}

resource "digitalocean_database_firewall" "postgres-firewall" {
  cluster_id = digitalocean_database_cluster.postgres-cluster.id

  rule {
    type  = "tag"
    value = digitalocean_tag.api.name
  }
}

resource "digitalocean_database_connection_pool" "postgres-pool" {
  cluster_id = digitalocean_database_cluster.postgres-cluster.id
  name       = "postgres-pool"
  mode       = "transaction"
  size       = 12
  db_name    = "meco"
  user       = "meco"
}

resource "digitalocean_database_db" "database" {
  cluster_id = digitalocean_database_cluster.postgres-cluster.id
  name       = "meco"
}

resource "digitalocean_database_user" "user" {
  cluster_id = digitalocean_database_cluster.postgres-cluster.id
  name       = "meco"
}