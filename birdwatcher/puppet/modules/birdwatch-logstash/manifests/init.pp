class birdwatch-logstash () {
    class { 'logstash':
        manage_repo => true,
        repo_version => '1.4'
    }
    file { '/etc/init.d/logstash':
        source => "puppet:///modules/birdwatch-logstash/logstash",
        notify => Service['logstash']
    }
    logstash::plugin {'percolate':
        ensure => 'present',
        type => 'filter',
        source => 'puppet:///modules/birdwatch-logstash/percolate.rb',
        notify => Service['logstash']
    }

    file { '/etc/logstash/conf.d/1_twitter.conf': 
        content => template('birdwatch-logstash/1_twitter.erb'),
        notify => Service['logstash']
    }
    file { '/etc/logstash/conf.d/1_file.conf': 
        source => 'puppet:///modules/birdwatch-logstash/1_file.conf',
        notify => Service['logstash']
    }
    file { '/etc/logstash/conf.d/2_percolate.conf': 
        content => template('birdwatch-logstash/2_percolate.erb'),
        notify => Service['logstash']
    }
    file { '/etc/logstash/conf.d/3_elastic.conf': 
        content => template('birdwatch-logstash/3_elastic.erb'),
    }
}