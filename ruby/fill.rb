require 'yaml'
require 'elasticsearch'
require 'cgi'
require 'nokogiri'

config = YAML.load_file('index.yml')
index = config['index']

client = Elasticsearch::Client.new log: true

client.transport.reload_connections!

if client.indices.exists index:index
    client.indices.delete index:index
end

client.indices.create index:index,
    body: {
        mappings: {
            birdsource: {
                properties: {
                    birds: {
                        type: "string",
                        index: "not_analyzed"
                    },
                    fulltext: {
                        type: "string"
                    }
                }
            }
        }
    }


Dir.glob('../downloaded/**/*').reject{|file| File.directory? file}.each do |file|
    puts "Indexing #{file}"    
    client.index index: index,
        type: 'birdsource',
        body: {
            fulltext: Nokogiri::HTML(CGI.unescapeHTML(IO.read(file).force_encoding("ISO-8859-1").encode("UTF-8"))).content
        }
end

