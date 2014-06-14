#!/usr/bin/env ruby

require 'yaml'
require 'elasticsearch'

config = YAML.load_file('index.yml')
index = config['index']

client = Elasticsearch::Client.new # log: true

client.transport.reload_connections!

result = client.search index: index,
    body: {
        query: {
            match_all: {}
        },   
        filter: {
            exists: { 
                field: "birds" 
            }
        }, 
        aggs: {
            birds: {
                terms: {
                    field:"birds",
                    size:0
                }
            }
        }
    }

puts "A total of #{result['hits']['total']} documents were tagged"

result['aggregations']['birds']['buckets'].each do |bird|
    puts "#{bird['key']}: #{bird['doc_count']}"
end
