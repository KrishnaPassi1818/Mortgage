#!/bin/bash
cd /home/krishna/Downloads/javaProject
set -a
source .env
set +a
cd shipment-service
exec mvn exec:java
