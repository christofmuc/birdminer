# Creating a local vagrant box
* cd into `birdwatcher`
* run `vagrant up`

# Creating an AWS instance
* Install the plugin

        vagrant plugin install vagrant-aws

* Make a copy of `aws.yaml_sample` as `aws.yaml`
* Start the instance

        vagrant up aws --provider=aws
