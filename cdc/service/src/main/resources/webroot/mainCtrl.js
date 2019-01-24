var app = angular.module('myApp', ["ngTable","ngResource","ngDialog"]);
app.config(['$httpProvider', function ($httpProvider) {
    $httpProvider.interceptors.push(function ($q,$rootScope) {
        return {
            'responseError': function (responseError) {
                $rootScope.loading = false;
                $rootScope.message = responseError.data.message;
                return $q.reject(responseError);
            }
        };
    });
}]);

app.controller('mainCtrl', function($scope,$interval,NgTableParams,$resource,ngDialog,$rootScope) {
    $rootScope.loading = false;
    $scope.address = {first_name:"First name", last_name: "Last name", email: "Email"};
    $scope.addresses = $resource("addressbook/:addressId",{addressId:'@id'},{
        'update': { method:'PUT' }
    });

    $scope.tableParams = new NgTableParams({
            page: 1,
            count: 9999,
            noPager: true
        }, {
            counts: [],
            total: 1,
            getData: function(params) {
                /*var queryParams = {page:params.page()-1 , size:params.count()};
                var sortingProp = Object.keys(params.sorting());
                if(sortingProp.length == 1){
                    queryParams["sort"] = sortingProp[0];
                    queryParams["sortDir"] = params.sorting()[sortingProp[0]];
                }
                return $scope.addresses.query(queryParams, function(data, headers) {
                    var totalRecords = headers("PAGING_INFO").split(",")[0].split("=")[1];
                    params.total(totalRecords);
                    console.log(params.total());
                    return data;
                }).$promise;*/

                return $scope.addresses.query(function(data) {
                    return data;
                }).$promise;
            }
        });

    $scope.confirmDelete = function(id){
        if (confirm("Do you really want to delete this contact?") == true) {
            $scope.addresses.delete({addressId:id}, function(){
                $scope.tableParams.reload();
            });

        }
    }

    $scope.addNewAddress = function(){
        $scope.address = {first_name:"First name", last_name: "Last name", email: "Email"};
        ngDialog.open({ template: 'newTemplateId',	scope: $scope, className: 'ngdialog-theme-default' });
    }

    $scope.editAddress = function(row){
        $scope.address.id = row.id;
        $scope.address.first_name = row.first_name;
        $scope.address.last_name = row.last_name;
        $scope.address.email = row.email;

        ngDialog.open({ template: 'editTemplateId',	scope: $scope, className: 'ngdialog-theme-default' });
    }

    $scope.save = function(){
        ngDialog.close('ngdialog1');
        $rootScope.loading = true;
        if(! $scope.address.id){
            $scope.createAddress();
        }else{
            $scope.updateAddress();
        }
    }

    $scope.createAddress = function(){
        $scope.addresses.save($scope.address, function(){
            $scope.tableParams.reload();
            $rootScope.loading = false;
        });
    }

    $scope.updateAddress = function(){
        $scope.addresses.update($scope.address, function(){
            $scope.tableParams.reload();
            $rootScope.loading = false;
        });
    }

    $scope.reload = $interval(function() {$scope.tableParams.reload()}, 10000);
});