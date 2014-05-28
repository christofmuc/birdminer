var connect = require('connect');
connect.createServer(
    connect.static("c:/users/christof/src/AngularDemo/app")
).listen(8080);
