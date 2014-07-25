class birdwatch-elasticsearchkibana() {
    class {'elasticsearch':
        manage_repo  => true,
        repo_version => '1.2',
        config => {
            'cluster.name' => 'birdwatch',
            'node.name' => 'vagrant',
            'index.number_of_shards' => 1,
            'index.number_of_replicas' => 0
        }
    }

    $es_host = hiera('elasticsearch.host')
    exec {'create_index':
        command => "/usr/bin/curl -XPUT \"http://${es_host}:9200/facebook/post/1\" -d'
            {
                \"nullValue\": \"create the Index\"
            }' && 
            /usr/bin/curl -XPUT 'http://${es_host}:9200/facebook/post/_mapping' -d '
            {
                \"post\" : {
                    \"properties\" : {
                        \"birds\" : {\"type\" : \"string\", \"index\" : \"not_analyzed\" },
                        \"locations\" : {\"type\" : \"string\", \"index\" : \"not_analyzed\" }
                    }
                }
            }'",
        onlyif => "/usr/bin/curl -XGET \"http://${es_host}:9200/facebook/post/_mapping\" -s | grep --quiet IndexMissingException"
    }

    elasticsearch::instance { 'birdmine': 
        config => { 'node.name' => 'vagrant' },
    }

    elasticsearch::plugin{'lmenezes/elasticsearch-kopf':
      module_dir => 'kopf',
      instances => 'birdmine',
    }

    # package {'git':} ->
    # class {'kibana3': 
    #     manage_git => false,
    #     manage_ws => false,
    #     config_default_route => '/dashboard/file/BirdWatcher.json'
    # }
    # file { '/opt/kibana3/src/app/dashboards/BirdWatcher.json':
    #     owner => 'www-data',
    #     source => "puppet:///modules/birdwatch-elasticsearchkibana/BirdWatcher.json"
    # }

    class {'apache':        
    }
    apache::vhost { '192.168.50.4':
        port             => '80',
        docroot          => '/vagrant/www'
    }
}