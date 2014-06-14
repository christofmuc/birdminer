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

        $http.post('http://localhost:9200/percolators/.percolator/_search',query).
            success(function (data, status, headers, config) {
                console.log("Preload done with: ");
                angular.forEach(data.hits.hits, function(val,key) {
                    var name = "";
                    if (val._source.location) {
                        name = val._source.location;
                    } else {
                        name = val._source.bird;
                    }

                    $scope.lookup[val._id.toLowerCase()] = name;
                });
                console.log($scope.lookup);
                $scope.refreshCandidateList();
            });
    };

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
    };

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
                            name: $scope.lookup[bucket.key.toLowerCase()], 
                            count:bucket.doc_count
                        });
                });

                angular.forEach(candidates, function (value, key) {
                    var documentId = value._source.documentId;

                    angular.forEach(value._source.percolators, function (value, key) {
                        $http.get('http://localhost:9200/percolators/.percolator/' + value).
                            success(function (data, status, headers, config) {
                                var payload = { 
                                    query: data._source.query, 
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
                                var location = data._source.location;
                                var bird = data._source.bird;
                                $http.post('http://localhost:9200/input/_search',
                                        payload).
                                    success(function (data, status, headers, config) {
                                        for (var i = 0; i < $scope.candidates.length; i++) {
                                            if ($scope.candidates[i].id === documentId) {
                                                if (bird) {
                                                    $scope.candidates[i].birds.push(bird);
                                                }
                                                if (location) {
                                                    $scope.candidates[i].locations.push(location);
                                                }
                                                angular.forEach(data.hits.hits[0].highlight.message, function(text, key) {
                                                    var escapedText = text; //$sce.trustAsHtml(text);
                                                    $scope.candidates[i].highlights.push(escapedText);
                                                    console.log($scope.candidates[i]);
                                                });
                                                // $scope.candidates[i].highlight = prepend($scope.candidates[i].highlight,
                                                //    $sce.trustAsHtml(data.hits.hits[0].highlight.text[0]));
                                            }
                                        }
                                        console.log($scope.candidates[i]);
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