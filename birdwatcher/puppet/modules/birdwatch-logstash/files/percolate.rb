# encoding: utf-8
# Percolate Filter
#
# This filter percolate the message against a given ES index and record the
# hit searches

require "logstash/filters/base"
require "logstash/namespace"

# The DNS filter performs a lookup (either an A record/CNAME record lookup
# or a reverse lookup at the PTR record) on records specified under the
# "reverse" and "resolve" arrays.
#
# The config should look like this:
#
#     filter {
#       dns {
#         type => 'type'
#         reverse => [ "source_host", "field_with_address" ]
#         resolve => [ "field_with_fqdn" ]
#         action => "replace"
#       }
#     }
#
# Caveats: at the moment, there's no way to tune the timeout with the 'resolv'
# core library.  It does seem to be fixed in here:
#
#   http://redmine.ruby-lang.org/issues/5100
#
# but isn't currently in JRuby.
class LogStash::Filters::Percolate < LogStash::Filters::Base
  config_name "percolate"
  milestone 1

  # Elasticsearch server to use
  config :server, :validate => :string, :required => true

  # ES Index to use
  config :index, :validate => :string, :required => true

  config :object_type, :validate => :string, :required => true

  # The field to query the index with
  config :field, :validate => :string

  public
  def register
    require 'elasticsearch'

    if @field.nil?
      @field = 'message'
    end

    @client = Elasticsearch::Client.new host: @server, log: false
    @client.transport.reload_connections!
  end # def register

  public
  def filter(event)
    return unless filter?(event)

    event['matches'] = []
    doc = { doc: { message: event[@field] } }
    result = @client.percolate index: @index, type: @object_type, body: { doc: { message: event[@field] } }

    # {"took"=>11, "_shards"=>{"total"=>5, "successful"=>5, "failed"=>0}, "total"=>1, "matches"=>[{"_index"=>"percolators", "_id"=>"Location_103"}]}
    result['matches'].each do |match|
      event['matches'] << match['_id']
    end

    filter_matched(event)
  end

end # class LogStash::Filters::Percolate
