#!/usr/bin/env bash

# Base installation
apt-get --yes update
apt-get --yes install build-essential
apt-get --yes install ruby

# Install Puppet modules used later
mkdir -p /etc/puppet/modules
puppet module install puppetlabs/apt
puppet module install elasticsearch-elasticsearch

# This is one of many Kibana puppet modules
puppet module install thejandroman-kibana3

# Fix Puppet Ruby installation on Ubuntu Lucid
# https://ask.puppetlabs.com/question/2147/could-not-find-a-suitable-provider-for-augeas/
# http://m0dlx.com/blog/Puppet__could_not_find_a_default_provider_for_augeas.html
# https://launchpad.net/~raphink/+archive/augeas
sudo add-apt-repository ppa:raphink/augeas
sudo apt-get install libaugeas0
sudo /opt/vagrant_ruby/bin/gem install ruby-augeas 

#sudo apt-get --yes install libaugeas-dev
#sudo apt-get install libxml2-dev



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
sudo cp /vagrant/td-agent.conf /etc/td-agent/td-agent.conf

# Fire up FluentD
sudo /etc/init.d/td-agent start
