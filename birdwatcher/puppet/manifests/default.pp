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
        'consumer_key' => '9Kg1TBr2R5zWYFbrb8KOwdtNq',
        'consumer_secret' => 'gOGhl3OjJ5NPWhjfQ38ZcpC29dlIIWDwXAJvIVevmiLIkDwQRo',
        'oauth_token' => '596284653-CjwQHdOXM9G8PrWiP5ri1RB05dkOEntbCfAya3fb',
        'oauth_token_secret' => '4BDQW5VXlxiMT5MKN3uvKmxe6G5aPG0vHyYVTSkM3mlKB',
        'output_format' => 'simple',
        'timeline' => 'sampling',
        'keyword' => '${hashtag}bird',
    },
    notify => Class['fluentd::service'],
}

fluentd::configfile {'exec':}
fluentd::source{'exec_in':
    configfile => 'exec',
    type => 'exec',    
    format => 'json',
    tag => 'facebook.post',
    config => {
        'command' => 'java -cp /vagrant/out:/vagrant/repository/restfb-1.6.14.jar info.alpenglow.IngestFromFacebook',
        'run_interval' => '1m',
    },
    notify => Class['fluentd::service'],
}

fluentd::configfile {'tail':}
fluentd::source{'tail_in':
    configfile => 'tail',
    type => 'tail',    
    format => 'json',
    tag => 'facebook.post',
    config => {
        'path' => '/home/vagrant/facebookPosts.txt',
        'pos_file' => '/var/log/td-agent/facebookPosts.pos',
        'time_key' => 'created_time',
    },
    notify => Class['fluentd::service'],
}

fluentd::configfile {'forward_twitter':}
fluentd::match {'forward_twitter_match':
    configfile => 'forward_twitter',
    pattern => 'input.twitter',
    type => 'elasticsearch',
    config => {
        'host' => '192.168.50.4',
        'port' => '9200',
        'index_name' => 'twitter',
        'type_name' => 'tweet',  
        'logstash_format' => true,      
    }
}

fluentd::configfile {'forward_facebook':}
fluentd::match {'forward_facebook_match':
    configfile => 'forward_facebook',
    pattern => 'facebook.post',
    type => 'elasticsearch',
    config => {
        'host' => '192.168.50.4',
        'port' => '9200',
        'index_name' => 'facebook',
        'type_name' => 'post',        
        'logstash_format' => true,
    }
}