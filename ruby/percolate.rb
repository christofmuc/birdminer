#!/usr/bin/env ruby

require 'yaml'
require 'elasticsearch'
require 'hashie'

@client = Elasticsearch::Client.new # log: true
@client.transport.reload_connections!

def tag_field_with_value(doc, tag, value)
    org = doc._source['tag']
    if org
        unless org.include? value
            org << value
        end
    else
        org = [value]
    end
    body = { doc: {} }
    body[:doc][tag] = org
    puts body
    @client.update index: 'birdwatch', type: 'birdsource', id: doc._id, body: body
end

birds = {}
locations = {}

puts "Get all birds"
result = @client.search index: 'birdwatch', type: '.percolator', from: 0, size: 10000, 
    body: {
        query: {
            constant_score: {
                filter: {
                    exists: {
                        field: "bird"
                    }
                }
            }
        }
    }
result = Hashie::Mash.new result
result.hits.hits.each do |bird|
    birds[bird._id] = {bird: bird._source.bird, synonyms: bird._source.synonyms}
end

puts "Get all locations"
result = @client.search index: 'birdwatch', type: '.percolator', from: 0, size: 10000, 
    body: {
        query: {
            constant_score: {
                filter: {
                    exists: {
                        field: "location"
                    }
                }
            }
        }
    }
result = Hashie::Mash.new result
result.hits.hits.each do |location|
    locations[location._id] = {location: location._source.location}
end

from = 0
remaining = 0
size = 100
loop do
    result = @client.search index: 'birdwatch', type: 'birdsource', from: from, size: size
    result = Hashie::Mash.new result
    from += size
    remaining = result.hits.total - (from + size)
    body = []
    result.hits.hits.each do |result|
        body << { percolate: { index: 'birdwatch', type: 'birdsource', id: result._id}}
    end
    matches = @client.mpercolate body: body
    matches['responses'].each_with_index do |match, idx|
        if match["matches"].length > 0
            match['matches'].each do |submatch|
                if birds[submatch['_id']]
                    puts "Found bird"
                    tag_field_with_value result.hits.hits[idx], 'birds', birds[submatch['_id']][:bird]
                end
                if locations[submatch['_id']]
                    puts "Found location #{locations[submatch['_id']][:location]}"
                    tag_field_with_value result.hits.hits[idx], 'locations', locations[submatch['_id']][:location]
                end
            end
        end
    end
    break if remaining <= 0
end

