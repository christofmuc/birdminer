function candidateController($scope, $http, $sce) {

    $scope.candidates = [];
    $scope.items = [];
    $scope.lookup = [];
    $scope.birds = [];
    $scope.locations = [];

    $scope.preloadAndStart = function() {
        var query = {
            from:0,
            size:10000,
            query: {
                match_all: {}
            }
        };

        $http.post('http://localhost:9200/birdwatch/.percolator/_search',query).
            success(function (data, status, headers, config) {
                console.log("Preload done with: ");
                birds = [];
                locations = [];
                angular.forEach(data.hits.hits, function(val,key) {
                    var id = val._id.toLowerCase();
                    var name = "";
                    if (val._source.bird) {
                         birds.push(val._id);
                        name = val._source.bird;
                    } else {
                        locations.push(val._id);
                        name = val._source.location;
                    }

                    $scope.lookup[id] = {
                        name:name,
                        query: val._source.query
                    };
                });
                console.log($scope.lookup);
                $scope.refreshCandidateList();
            });
    }

    $scope.returnTotalCandidates = function () {
        console.log('returnTotalCandidates executes');
        return $scope.candidates.length;
    };

    $scope.returnTotalItems = function () {
        console.log('returnTotalItems executes');
        return $scope.items.length;
    };

    function prepend(array, value) {
        if (value !== undefined) {
            if (array !== undefined) {
                return array + '\r\n' + value;
            }
            return value;
        } else {
            return array;
        }

    }

    $scope.filterByKey = function(key) {
        console.log("Filter by key: "+key);
        $scope.refreshCandidateList(key);
    }

    $scope.refreshCandidateList = function (key) {
        $scope.clearCandidates();
        var filter = {
                and: [
                    {
                        exists:{
                            field:"birds"
                        }
                    },
                    {
                        exists:{
                            field:"locations"
                        }
                    }
                ]
            };

        if (key) {
            if (birds.indexOf(key) != -1) {
                filter.and.push( 
                {
                    term: {
                        birds: key,
                    }
                });                
            } else if (locations.indexOf(key != -1)) {
                filter.and.push( 
                {
                    term: {
                        locations: key,
                    }
                });                                
            }
        }

        var search_payload = {
            size: 500,
            query: {
                match_all : {}
            },
            aggs: {
                both_only: {
                    filter : filter,
                    aggs: {
                        birds: {
                            terms: {
                                field: "birds",
                                size: 0
                            }                            
                        },
                        locations: {
                            terms: {
                                field: "locations",
                                size: 0
                            }                                                        
                        }
                    }
                }
            },
            filter: filter
        };
        console.log(JSON.stringify(search_payload));
        $http.post('http://localhost:9200/birdwatch/_search', search_payload).
            success(function (data, status, headers, config) {
                console.log("Status is", status);
                var candidates = data.hits.hits;
                angular.forEach(data.aggregations.both_only.birds.buckets, function(bucket, key) {
                    $scope.items.push(
                        {
                            key: bucket.key, 
                            name: $scope.lookup[bucket.key.toLowerCase()].name, 
                            count: bucket.doc_count
                        });
                });

                angular.forEach(candidates, function (value, key) {
                    var documentId = value._id;
                    var birds = value._source.birds;
                    var locations = value._source.locations
                    var percs = birds.concat(locations);

                    angular.forEach(percs, function (tagName, key) {
                        var lookup_key = tagName.toLowerCase();
                        var lookup = $scope.lookup[lookup_key];
                        var payload = { 
                            query: lookup.query, 
                            filter: { 
                                ids: { 
                                    values: [ documentId ] 
                                } 
                            }, 
                            highlight: { 
                                fields: { 
                                    fulltext: {}
                                }
                            } 
                        };

                        console.log(JSON.stringify(payload));

                        $http.post('http://localhost:9200/birdwatch/birdsource/_search', payload)
                            .success(function (data, status, headers, config) {
                                angular.forEach($scope.candidates, function(candidate, idx) {
                                    if (candidate.id === documentId) {
                                        if (birds.indexOf(tagName) != -1) {
                                            candidate.birds.push(lookup.name);
                                        }
                                        if (locations.indexOf(tagName) != -1) {
                                            candidate.locations.push(lookup.name);
                                        }
                                        angular.forEach(data.hits.hits[0].highlight.fulltext, function(text, key) {
                                            candidate.highlights.push(text);
                                        });
                                    }

                                });
                            });
                    });

                    $scope.candidates.push(
                        { 
                            id: documentId, 
                            birds:[], 
                            locations:[],
                            highlights:[],
                            file: value._source.file, 
                            percolators: value._source.percolators 
                        }
                    );
                });
            }).
            error(function (data, status, headers, config) {
                console.log("Error! is ", status);
                // called asynchronously if an error occurs
                // or server returns response with status
                // code outside of the <200, 400) range
            });
        return true;
    };

    $scope.showCandidate = function () {
        console.log('showCandidate executes');
    };

    $scope.showItems = function () {
        console.log('showItems executes');
    };

    $scope.clearCandidates = function () {
        console.log('clear executes');
        $scope.candidates = [];
    }

    $scope.clearItems = function () {
        console.log('clear executes');
        $scope.items = [];
    }
}