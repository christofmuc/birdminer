require 'hashie'

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
    @client.update index: @index, type: @type, id: doc._id, body: body
end

birds = {}
locations = {}

puts "Get all birds"
result = @client.search index: @index, type: '.percolator', from: 0, size: 10000, 
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
result = @client.search index: @index, type: '.percolator', from: 0, size: 10000, 
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
    result = @client.search index: @index, type: @type, from: from, size: size
    result = Hashie::Mash.new result
    from += size
    remaining = result.hits.total - (from + size)
    result.hits.hits.each do |result|
        match = @client.percolate index: @index, type: @type, id: result._id
        if match["matches"].length > 0
            match['matches'].each do |submatch|
                if birds[submatch['_id']]
                    puts "Found bird #{birds[submatch['_id']][:bird]}"
                    tag_field_with_value result, 'birds', birds[submatch['_id']][:bird]
                end
                if locations[submatch['_id']]
                    puts "Found location #{locations[submatch['_id']][:location]}"
                    tag_field_with_value result, 'locations', locations[submatch['_id']][:location]
                end
            end
        end
    end
    break if remaining <= 0
end

