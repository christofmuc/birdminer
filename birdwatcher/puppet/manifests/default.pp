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

exec {'create_index':
    command => "/usr/bin/curl -XPUT 'http://localhost:9200/facebook/post/_mapping' -d '
        {
            \"post\" : {
                \"properties\" : {
                    \"birds\" : {\"type\" : \"string\", \"index\" : \"not_analyzed\" },
                    \"locations\" : {\"type\" : \"string\", \"index\" : \"not_analyzed\" }
                }
            }
        }'",
    onlyif => "/usr/bin/curl -XGET \"http://192.168.50.4:9200/facebook/post/_mapping\" -s | grep --quiet IndexMissingException"
}

package {'git':} ->
class {
    'kibana3': manage_git => false,
    config_default_route => '/dashboard/file/BirdWatcher.json'
}
file { '/opt/kibana3/src/app/dashboards/BirdWatcher.json':
    source => "puppet:///kibana/BirdWatcher.json",
    owner => 'www-data'
}

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
        'id_key' => 'id',
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
        #'time_key' => 'time', # Don't do this, as fluentd then seems to swallow the time stamp
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
        'id_key' => 'facebookID',
    }
}