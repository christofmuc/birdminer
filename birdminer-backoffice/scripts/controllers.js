function candidateController($scope, $http, $sce) {

    $scope.candidates = [];

    $scope.returnTotalCandidates = function () {
        console.log('returnTotalCandidates executes');
        return $scope.candidates.length;
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

    $scope.refreshCandidateList = function () {
        $scope.clearCandidates();
        $http({method: 'POST', url: 'http://localhost:9200/result/_search'}).
            success(function (data, status, headers, config) {
                console.log("Status is", status);
                var candidates = data.hits.hits;
                console.log(data);
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
                                                $scope.candidates[i].birds.push(bird);
                                                $scope.candidates[i].locations.push(location);
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

    $scope.clearCandidates = function () {
        console.log('clear executes');
        $scope.candidates = [];
    }
}