class { 'elasticsearch':
  version => '1.2.1'
}

elasticsearch::instance { 'birdmine': }

elasticsearch::plugin{'lmenezes/elasticsearch-kopf':
  module_dir => 'kopf',
  instances => 'birdmine'
}


