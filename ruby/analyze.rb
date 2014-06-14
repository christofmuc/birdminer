#!/usr/bin/env ruby
require 'yaml'
require 'elasticsearch'
require 'pp'

if ARGV.length < 1
    abort("Usage: ./analyze.rb <search template name>")
end

config = YAML.load_file('index.yml')
index = config['index']
template = config['searches'][ARGV[0]]
limit = false
highlight = false
if ARGV[1] == 'limit'
    limit = true
end
if ARGV[2] == 'highlight'
    highlight = true
end

if template == nil
    abort "Unknown template: #{ARGV[0]}"
end

client = Elasticsearch::Client.new # log: true

client.transport.reload_connections!

def replace_birdname(hash,name,newhash)
    hash.each do |key, value|
        if value == '<birdname>'
            newhash[key] = name
        elsif value.is_a?(Hash)
            newhash[key] = {}
            replace_birdname(value, name, newhash[key])
        else
            newhash[key] = value
        end
    end
end

File.open('../input/LR_USA_List_Flat_20081103.txt').each do |line|
    matchdata = /^\s(\S.*)/.match(line)
    if matchdata 
        birdname = matchdata[1].force_encoding("ISO-8859-1").encode("UTF-8")
        query = {}
        replace_birdname(template, birdname, query)    
        pp query
        response = client.search index:index,
            body: {
                query: query,
                highlight: {
                    fields: {
                        fulltext:{}
                    }
                }
            }

        response['hits']['hits'].each do |hit|
            puts hit['_id']
            birds = hit['_source']['birds']
            birds = [] if birds == nil
            unless birds.include? birdname
                birds << birdname
            end
            client.update index: index,
                type: 'birdsource',
                id: hit['_id'],
                body: {
                    doc: {
                        birds: birds
                    }
                }
            if highlight
                hit['highlight'].each do |h|
                    puts h
                end
            end
            if limit
                abort
            end
        end
    end
end
