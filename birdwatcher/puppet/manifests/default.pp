class {'apt':
    always_apt_update => true,
}

class {'elasticsearch':
    manage_repo  => true,
    repo_version => '1.2',
    config => {'cluster.name' => 'birdwatch'}
}

elasticsearch::instance { 'birdmine': 
    config => { 'node.name' => 'vagrant' },
}

elasticsearch::plugin{'lmenezes/elasticsearch-kopf':
  module_dir => 'kopf',
  instances => 'birdmine',
}

package {'git':} ->
class { 'kibana3': manage_git => false}

include ::fluentd

package {'libcurl4-gnutls-dev':} ->
fluentd::install_plugin { 'elasticsearch': 
    plugin_type => 'gem',
    plugin_name => 'fluent-plugin-elasticsearch',
}

package {'g++':} ->
package {'libssl-dev':} ->
fluentd::install_plugin { 'fluent-plugin-twitter': 
    plugin_type => 'gem',
    plugin_name => 'fluent-plugin-twitter',
}

fluentd::configfile {'twitter':}
fluentd::source{'twitter_in':
    configfile => 'twitter',
    type => 'twitter',    
    tag => 'input.twitter',
    config => {
        'consumer_key' => hiera('twitter.consumer_key'),
        'consumer_secret' => hiera('twitter.consumer_secret'),
        'oauth_token' => hiera('twitter.oauth_token'),
        'oauth_token_secret' => hiera('twitter.oauth_token_secret'),
        'output_format' => 'simple',
        'timeline' => 'sampling',
        'keyword' => '${hashtag}bird,${hashtag}birding',
    },
    notify => Class['fluentd::service'],
}
fluentd::match {'forward_twitter_match':
    configfile => 'twitter',
    pattern => 'input.twitter',
    type => 'elasticsearch',
    config => {
        'host' => '192.168.50.4',
        'port' => '9200',
        'index_name' => 'twitter',
        'type_name' => 'tweet',  
    }
}

fluentd::configfile {'exec':}
fluentd::source{'exec_in':
    configfile => 'exec',
    type => 'exec',    
    format => 'json',
    tag => 'facebook.post',
    config => {
        'command' => 'java -cp /vagrant/out:/vagrant/repository/fluent-logger-0.2.11.jar:/vagrant/repository/gson-2.2.4.jar:/vagrant/repository/javassist-3.16.1-GA.jar:/vagrant/repository/jdbm-2.4.jar:/vagrant/repository/msgpack-0.6.7.jar:/vagrant/repository/restfb-1.6.14.jar:/vagrant/repository info.alpenglow.IngestFromFacebook',
        'run_interval' => '1m',
    },
    notify => Class['fluentd::service'],
}
fluentd::match {'forward_facebook_match':
    configfile => 'exec',
    pattern => 'facebook.post',
    type => 'elasticsearch',
    config => {
        'host' => '192.168.50.4',
        'port' => '9200',
        'index_name' => 'facebook',
        'type_name' => 'post',        
    }
}