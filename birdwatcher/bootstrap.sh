#!/usr/bin/env bash

# Base installation
#apt-get --yes update
#apt-get --yes install build-essential
#apt-get --yes install ruby

# Install Puppet modules used later
mkdir -p /etc/puppet/modules
if ! puppet module list | grep -q puppetlabs-apt; then
    puppet module install puppetlabs/apt;
fi
if ! puppet module list | grep -q puppetlabs-concat; then
    puppet module install puppetlabs-concat;
fi
if ! puppet module list | grep -q puppetlabs-apache; then
    puppet module install puppetlabs-apache;
fi
if ! puppet module list | grep -q srf-fluentd; then
    puppet module install srf-fluentd;
fi
if ! puppet module list | grep -q puppetlabs-stdlib; then
    puppet module install puppetlabs-stdlib;
fi
if ! puppet module list | grep -q puppetlabs-vcsrepo; then
    puppet module install puppetlabs-vcsrepo;
fi
if ! puppet module list | grep -q elasticsearch-elasticsearch; then
    puppet module install elasticsearch-elasticsearch;
fi
if ! puppet module list | grep -q thejandroman-kibana3; then
    puppet module install thejandroman-kibana3;
fi

# Fix Puppet Ruby installation on Ubuntu Lucid
# https://ask.puppetlabs.com/question/2147/could-not-find-a-suitable-provider-for-augeas/
# http://m0dlx.com/blog/Puppet__could_not_find_a_default_provider_for_augeas.html
# https://launchpad.net/~raphink/+archive/augeas
#sudo add-apt-repository ppa:raphink/augeas
#sudo apt-get update
#sudo apt-get --yes install libaugeas0=1.1.0-0ubuntu1~raphink1~lucid1
#sudo apt-get --yes install libaugeas-dev=1.1.0-0ubuntu1~raphink1~lucid1
#sudo apt-get --yes install libxml2-dev
#sudo /opt/vagrant_ruby/bin/gem install ruby-augeas

 # Fix regular Ruby version by installing RVM and updating to 1.9.3
 #Actually this was not required as fluentd ships it's own ruby 1.9.3. Surprise!
 #sudo curl -sSL https://get.rvm.io | bash -s stable
 #source /home/vagrant/.rvm/scripts/rvm
 #sudo rvm install 1.9.3



# Install Fluentd in the td-agent version
#apt-get --yes install curl
#curl -L http://toolbelt.treasuredata.com/sh/install-ubuntu-lucid.sh | sh

# Install the Twitter Plugin for fluentd
#apt-get --yes install libssl-dev
#sudo /usr/lib/fluent/ruby/bin/fluent-gem install fluent-plugin-twitter

# Install Elasticsearch Plugin for fluentd
#sudo apt-get --yes install libcurl4-gnutls-dev
#sudo /usr/lib/fluent/ruby/bin/fluent-gem install fluent-plugin-elasticsearch

# Configure fluentd
#if [ ! -f ~/td-agent.conf.original ]; then cp /etc/td-agent/td-agent.conf ~/td-agent.conf.original; fi;
#sudo cp /vagrant/td-agent.conf /etc/td-agent/td-agent.conf

# Fire up FluentD
#sudo /etc/init.d/td-agent restart
