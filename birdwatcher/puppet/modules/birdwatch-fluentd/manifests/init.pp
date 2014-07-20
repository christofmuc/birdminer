class birdwatch-fluentd() {

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

    fluentd::configfile {'percolate_them':}
    fluentd::source{'percolate documents':
        configfile => 'percolate_them',
        type => 'exec',
        format => 'json',
        tag => 'percolate.log',
        config => {
            'command' => 'java -cp /vagrant/repository/elasticsearch-1.2.2.jar:/vagrant/javabirder/properties:/vagrant/out/:/vagrant/repository/lucene-core-4.8.1.jar info.alpenglow.PercolateAllDocuments',
            #'time_key' => 'time', # Don't do this, as fluentd then seems to swallow the time stamp
            'run_interval' => '1m',
        },
        notify => Class['fluentd::service'],
    }
    fluentd::match{'percolate log':
        configfile => 'percolate_them',
        pattern => 'percolate.log',
        type => 'stdout',
        notify => Class['fluentd::service'],
    }
}