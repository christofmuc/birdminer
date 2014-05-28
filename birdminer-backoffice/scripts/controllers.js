function candidateController($scope, $http) {

    $scope.candidates = [
        {message: 'walk the dog', done: false},
        {message: 'feed the cat', done: false}
    ];

    $scope.returnTotalCandidates = function () {
        console.log ('returnTotalCandidates executes');
        return $scope.candidates.length;
    };

    $scope.refreshCandidateList= function () {
        console.log ('refreshCandidateList executes');
        $scope.clearCandidates();
        $http({method: 'POST', url: 'http://localhost:9200/result/_search'}).
            success(function(data, status, headers, config) {
                console.log("Status is", status);
                var candidates = data.hits.hits;
                console.log(data);
                angular.forEach(candidates, function(value, key) {
                    $scope.candidates.push({ file: value._source.file, perculators: value._source.perculators });
                });
            }).
            error(function(data, status, headers, config) {
                console.log("Error! is ", status);
                // called asynchronously if an error occurs
                // or server returns response with status
                // code outside of the <200, 400) range
            });
        return true;
    };

    /*$scope.addNewTodo = function () {
        console.log ('addNewTodo executes');
        var nothing = $scope.todos.length;
        var nothing2  = nothing;
        if ($scope.newTodoText.length){
            $scope.todos.push ( {todoItem: $scope.newTodoText , done: false});
            $scope.newTodoText = '';
        }


    }                             */

    $scope.clearCandidates = function () {
        console.log ('clear executes');
        $scope.candidates = [];
    }
}