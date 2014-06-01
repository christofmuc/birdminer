function candidateController($scope, $http, $sce) {

    $scope.candidates = [];
    $scope.items = [];

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
                console.log(data);
                angular.forEach(data.aggregations.birds.buckets, function(bucket, key) {
                    $scope.items.push({key: bucket.key, count:bucket.doc_count});
                });

                angular.forEach(candidates, function (value, key) {
                    var documentId = value._id;

                    angular.forEach(value._source.percolators, function (value, key) {
                        $http({method: 'GET', url: 'http://localhost:9200/birding/.percolator/' + value}).
                            success(function (data, status, headers, config) {
                                var payload = { query: data._source.query, filter: { ids: { values: [ documentId ] } }, highlight: { fields: { text: {}}} };
                                var location = data._source.location;
                                var bird = data._source.bird;
                                $http.post('http://localhost:9200/result/bird_candidate/_search',
                                        payload).
                                    success(function (data, status, headers, config) {
                                        console.log("highlight" + data.hits.hits[0].highlight.text[0]);
                                        for (var i = 0; i < $scope.candidates.length; i++) {
                                            if ($scope.candidates[i].id === documentId) {
                                                if (bird) {
                                                    $scope.candidates[i].birds.push(bird);
                                                }
                                                if (location) {
                                                    $scope.candidates[i].locations.push(location);
                                                }
                                                $scope.candidates[i].highlight = prepend($scope.candidates[i].highlight,
                                                    $sce.trustAsHtml(data.hits.hits[0].highlight.text[0]));
                                            }
                                        }
                                    });
                            });
                    });

                    $scope.candidates.push(
                        { 
                            id: documentId, 
                            birds:[], 
                            locations:[],
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