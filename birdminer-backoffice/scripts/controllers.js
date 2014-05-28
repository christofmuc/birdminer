function candidateController($scope) {

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
        return $scope.candidates.length;
    };

    /*$scope.addNewTodo = function () {
        console.log ('addNewTodo executes');
        var nothing = $scope.todos.length;
        var nothing2  = nothing;
        if ($scope.newTodoText.length){
            $scope.todos.push ( {todoItem: $scope.newTodoText , done: false});
            $scope.newTodoText = '';
        }


    }

    $scope.clearFinishedTodos = function () {
        console.log ('clearFinishedTodos executes');
        $scope.todos = _.filter($scope.todos, function (todo) {return !todo.done})
    }
     */
}