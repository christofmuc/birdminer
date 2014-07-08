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
