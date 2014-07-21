class {'apt':
    always_apt_update => true,
}

class {'birdwatch-elasticsearchkibana':}
class {'birdwatch-fluentd':}

# class {'birdwatch-logstash':}

