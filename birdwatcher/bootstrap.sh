#!/usr/bin/env bash

# Base installation
apt-get --yes update
apt-get --yes install build-essential
apt-get --yes install ruby

# Install Fluentd in the td-agent version
apt-get --yes install curl
curl -L http://toolbelt.treasuredata.com/sh/install-ubuntu-lucid.sh | sh

# Install the Twitter Plugin for fluentd
apt-get --yes install libssl-dev
sudo /usr/lib/fluent/ruby/bin/fluent-gem install fluent-plugin-twitter

# Install Elasticsearch Plugin for fluentd
sudo apt-get --yes install libcurl4-gnutls-dev
sudo /usr/lib/fluent/ruby/bin/fluent-gem install fluent-plugin-elasticsearch

# Configure fluentd
if [ ! -f ~/td-agent.conf.original ]; then cp /etc/td-agent/td-agent.conf ~/td-agent.conf.original; fi;
cat ~/td-agent.conf.original /vagrant/twitter-source-fluentd.txt > td-agent.conf
sudo cp td-agent.conf /etc/td-agent/td-agent.conf

# Fire up FluentD
sudo /etc/init.d/td-agent start
