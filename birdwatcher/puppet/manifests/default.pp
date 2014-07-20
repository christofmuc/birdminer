class {'apt':
    always_apt_update => true,
}

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

elasticsearch::instance { 'birdmine': 
    config => { 'node.name' => 'vagrant' },
}

elasticsearch::plugin{'lmenezes/elasticsearch-kopf':
  module_dir => 'kopf',
  instances => 'birdmine',
}

package {'git':} ->
class {
    'kibana3': manage_git => false,
    config_default_route => '/dashboard/file/BirdWatcher.json'
}
file { '/opt/kibana3/src/app/dashboards/BirdWatcher.json':
    owner => 'www-data',
    source => "puppet:///kibana/BirdWatcher.json"
}

class {'birdwatch-fluentd':}

class {'birdwatch-logstash':}

