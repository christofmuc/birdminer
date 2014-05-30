require 'yaml'
require 'elasticsearch'

config = YAML.load_file('index.yml')
index = config['index']

client = Elasticsearch::Client.new # log: true

client.transport.reload_connections!

File.open('../input/LR_USA_List_Flat_20081103.txt').each do |line|
    matchdata = /^\s(\S.*)/.match(line)
    if matchdata 
        birdname = matchdata[1].force_encoding("ISO-8859-1").encode("UTF-8")
        puts birdname
        response = client.search index:index,
            body: {
                query: {
                    match_phrase: {
                        fulltext: birdname
                    }
                },
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
        end
    end
end
