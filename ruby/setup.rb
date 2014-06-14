#!/usr/bin/env ruby

require 'yaml'
require 'elasticsearch'
require 'cgi'
require 'nokogiri'

config = YAML.load_file('index.yml')
@index = config['index']
@type = config['type']

@client = Elasticsearch::Client.new # log: true

@client.transport.reload_connections!

if @client.indices.exists index:@index
    @client.indices.delete index:@index
end

birdsource = { properties: {
                    birds: {
                        type: "string",
                        index: "not_analyzed"
                    },
                    locations: {
                        type: "string",
                        index: "not_analyzed"
                    },
                    fulltext: {
                        type: "string"
                    }
                }
            }

mapping = { mappings: {} }

mapping[:mappings][@type] = birdsource

@client.indices.create index:@index, body: mapping

puts "Indexing all documents"
require('./lib/fill.rb')

puts "Setting up percolators"
require('./lib/createperc.rb')

puts "Tagging all data"
require ('./lib/percolate.rb')