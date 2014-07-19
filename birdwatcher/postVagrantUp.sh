#!/usr/bin/env bash

# Deploy the Kibana dashboards to the file system
sudo cp /vagrant/kibana/* /opt/kibana3/src/app/dashboards/
