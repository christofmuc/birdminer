#!/usr/bin/env bash

# Make sure we have mappings in place for the percolator result fields - else, ElasticSearch breaks at spaces
curl -XPUT 'http://localhost:9200/facebook/post/1' -d '
{
  "nullValue": "create the Index"
}
'
curl -XPUT 'http://localhost:9200/facebook/post/_mapping' -d '
{
    "post" : {
        "properties" : {
            "birds" : {"type" : "string", "index" : "not_analyzed" },
            "locations" : {"type" : "string", "index" : "not_analyzed" }
        }
    }
}
'