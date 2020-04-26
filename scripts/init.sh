#!/bin/bash

directory=$1 
user=$2 

# directory for geolite2 database
sudo mkdir $directory
sudo chmod 755 $directory
sudo chown -R $user:$user $directory

echo "directory created: $directory, user: $user"