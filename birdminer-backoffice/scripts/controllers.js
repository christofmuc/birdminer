function candidateController($scope, $http, $sce) {

    $scope.candidates = [];
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

        $http.post('http://localhost:9200/percolators/.percolator/_search',query).
            success(function (data, status, headers, config) {
                console.log("Preload done with: ");
                angular.forEach(data.hits.hits, function(val,key) {
                    var id = val._id.toLowerCase();
                    var name = "";
                    if (id.indexOf("bird_")==0) {
                        name = val._source.bird;
                    }
                    else if (id.indexOf("location_")==0) {
                        name = val._source.location;
                    }

                    $scope.lookup[id] = {
                        key:id,
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

    $scope.returnTotalBirds = function () {
        console.log('returnTotalBirds executes');
        return $scope.birds.length;
    };

    $scope.returnTotalLocations = function () {
        console.log('returnTotalLocations executes');
        return $scope.locations.length;
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

    $scope.filterByBird = function(bird) {
        console.log("Filter by bird: "+bird);
        $scope.birdFilter = $scope.lookup[bird];
        $scope.refreshCandidateList();
    }

    $scope.clearBird = function() {
        $scope.birdFilter = null;
        $scope.refreshCandidateList();
    }

    $scope.clearLocation = function() {
        $scope.locationFilter = null;
        $scope.refreshCandidateList();
    }

    $scope.filterByLocation = function(location) {
        console.log("Filter by location: "+location);
        $scope.locationFilter = $scope.lookup[location];
        $scope.refreshCandidateList();
    }

    $scope.refreshCandidateList = function () {
        $scope.clearCandidates();
        $scope.clearBirds();
        $scope.clearLocations();
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

        if ($scope.birdFilter) {
            filter.and.push({
                term: {
                    birds: $scope.birdFilter.key,
                }
            });
        }

        if ($scope.locationFilter) {
            filter.and.push({
                term: {
                    locations: $scope.locationFilter.key,
                }
            });
        }

        var search_payload = {
            size: 10,
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
        $http.post('http://localhost:9200/result/_search', search_payload).
            success(function (data, status, headers, config) {
                console.log("Status is", status);
                var candidates = data.hits.hits;
                angular.forEach(data.aggregations.both_only.birds.buckets, function(bucket, key) {
                    $scope.birds.push(
                        {
                            key: bucket.key,
                            name: $scope.lookup[bucket.key.toLowerCase()].name,
                            count: bucket.doc_count
                        });
                });
                angular.forEach(data.aggregations.both_only.locations.buckets, function(bucket, key) {
                    $scope.locations.push(
                        {
                            key: bucket.key,
                            name: $scope.lookup[bucket.key.toLowerCase()].name,
                            count: bucket.doc_count
                        });
                });

                angular.forEach(candidates, function (value, key) {
                    var documentId = value._source.documentId;

                    angular.forEach(value._source.birds, function (tagName, key) {
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
                                    message: {}
                                }
                            }
                        };
                        console.log(JSON.stringify(payload));
                        $http.post('http://localhost:9200/input/_search', payload)
                            .success(function (data, status, headers, config) {
                                angular.forEach($scope.candidates, function(candidate, idx) {
                                    if (candidate.id === documentId) {
                                        candidate.birds.push($scope.lookup[tagName.toLowerCase()].name);
                                        angular.forEach(data.hits.hits[0].highlight.fulltext, function(text, key) {
                                            candidate.highlights.push(text);
                                        });
                                    }
                                });
                            });
                    });

                    angular.forEach(value._source.locations, function (tagName, key) {
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
                                    message: {}
                                }
                            }
                        };
                        console.log(JSON.stringify(payload));
                        $http.post('http://localhost:9200/input/_search', payload)
                            .success(function (data, status, headers, config) {
                                angular.forEach($scope.candidates, function(candidate, idx) {
                                    if (candidate.id === documentId) {
                                        candidate.locations.push($scope.lookup[tagName.toLowerCase()].name);
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

    $scope.showBirds = function () {
        console.log('showBirds executes');
    };

    $scope.showLocations = function () {
        console.log('showLocations executes');
    };

    $scope.clearCandidates = function () {
        console.log('clear executes');
        $scope.candidates = [];
    }

    $scope.clearBirds = function () {
        console.log('clear executes');
        $scope.birds = [];
    }

    $scope.clearLocations = function () {
        console.log('clear locations executes');
        $scope.locations = [];
    }
}