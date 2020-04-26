#!/bin/bash

directory=$1 
user=$2 

# directory for geolite2 database
mkdir $directory
chmod 755 $directory
chown -R $user:$user $directory

echo "directory created: $directory, user: $user"