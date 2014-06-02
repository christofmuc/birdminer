function candidateController($scope, $http, $sce) {

    $scope.candidates = [];
    $scope.items = [];
    $scope.lookup = [];

    $scope.preloadAndStart = function() {
        var query = {
            from:0,
            size:10000,
            query: {
                match_all: {}
            }
        };

        $http.post('http://localhost:9200/birding/.percolator/_search',query).
            success(function (data, status, headers, config) {
                console.log("Preload done with: ");
                angular.forEach(data.hits.hits, function(val,key) {
                    var id = val._id.toLowerCase();
                    var name = "";
                    if (val._source.location) {
                        name = val._source.location;
                    } else {
                        name = val._source.bird;
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
        var search_payload = {
            query: {
                match_all : {}
            },
            aggs: {
                birds: {
                    terms: {
                        field: "percolators",
                        size: 0
                    }
                }
            }
        };
        if (key) {
            search_payload.filter = 
            {
                term: {
                    percolators: key,
                }
            };
        }
        console.log(search_payload);
        $http.post('http://localhost:9200/result/_search', search_payload).
            success(function (data, status, headers, config) {
                console.log("Status is", status);
                var candidates = data.hits.hits;
                angular.forEach(data.aggregations.birds.buckets, function(bucket, key) {
                    $scope.items.push(
                        {
                            key: bucket.key, 
                            name: $scope.lookup[bucket.key.toLowerCase()].name, 
                            count: bucket.doc_count
                        });
                });

                angular.forEach(candidates, function (value, key) {
                    var documentId = value._id;

                    angular.forEach(value._source.percolators, function (value, key) {
                        var lookup_key = value.toLowerCase();
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
                                    text: {}
                                }
                            } 
                        };

                        $http.post('http://localhost:9200/result/bird_candidate/_search', payload)
                            .success(function (data, status, headers, config) {
                                angular.forEach($scope.candidates, function(candidate, idx) {
                                    if (candidate.id === documentId) {
                                        if (lookup_key.indexOf("bird_") == 0) {
                                            candidate.birds.push(lookup.name);
                                        }
                                        if (lookup_key.indexOf("location_") == 0) {
                                            candidate.locations.push(lookup.name);
                                        }
                                        angular.forEach(data.hits.hits[0].highlight.text, function(text, key) {
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