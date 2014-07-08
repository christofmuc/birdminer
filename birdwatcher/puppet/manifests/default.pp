package {'git':}

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

class { 'kibana3': manage_git => false}