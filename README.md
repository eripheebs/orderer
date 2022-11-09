## Restaurant Orderer

This app intends to be an ordering tool for a restaurant.
The app is split into Handler, Service and Database layers. The app uses Akka's libraries to implement the Actor model as the concurrency model. It creates a new actor for each request.
The app uses postgresql db - the data structure for this use case would generally be well defined from the start and unlikely to change often. 

If I had more time, I would have kept the client code behind interfaces so that the app would be more agnostic of the clients I used, especially the akka toolkits because they are very integrated into the code now and would take more effort to extract. I would make a wrapper class that would handle generation of the actors I could wrap each route in (it is not an elegant solution right now). I also would have adding more robust unit testing in the Handler layer - I was running out of time when I got to the handler layer so I prioritised getting it working e2e testing the happy paths. I would have cleaned up the injected dependencies to include more configuration, a better logger, and passed clients that are more generic. I also would have liked to have been more explicit with error handling at each layer, and spent time on helpful api error responses. :)

### Set up to run locally

- build tooling  `sbt` (https://www.scala-sbt.org)
- compatibility: Scala v 2.13.7
- the config for that points to the DBs is in `main/resources/application.conf`
- install postgresql. For installation instructions, see: http://www.postgresql.org/download/. or for homebrew users: https://wiki.postgresql.org/wiki/Homebrew
- create the development and test databases

```
$ psql postgres
$ createdb restaurant
$ createdb restaurant_test
````

#### Build and run the app

```
$ sbt run
```

### Running Tests

```
$ sbt test
```

### Usage

##### read orders by ID

`GET /orders/{id}`

Return Object
I interpreted deleted as either cancelled or fulfilled Orders as that is what woud be most important to keep track of, so they are properties on the Order.

```
    Order {
        id: Long,
        // dishTypeName: Dish types are the type of dish ordered e.g. "CHICKEN_NUGGETS", we can get
        // the full name of the dish from the type.
        dishTypeName: String, 
        // orderTime: time of Order
        orderTime: LocalDateTime,
        // expectedTime: time the order is expected by AKA when it was promised to be delivered by
        // to the customer. This is calculated at order creation by OrderTime + DishType.maxCookingTimeInMin
        expectedTime: LocalDateTime,
        tableNumber: Int,
        // fulfilled: whether or not the order was fulfilled
        fulfilled: Boolean,
        fulfilledTime: Option[LocalDateTime],
        // cancelled: if the order was cancelled before it was fulfilled.
        cancelled: Boolean,
        cancelledTime: Option[LocalDateTime]
    }
```

```
$ curl -X GET http://localhost:8080/orders/<id>

```

##### read orders by table number

`GET /orders/by-table-number/{id}`

```
$ curl -X GET http://localhost:8080/orders/by-table-number/<id>

```

##### add orders

```
POST /orders Order[]

Order
    dishTypeName: String
    tableNumber: Int
```

```
curl -X POST -d '{"orders": [ {"dishTypeName": "<dish name>", "tableNumber": <table number>} ] }' -H "Content-Type: application/json" http://localhost:8080/orders
```

##### update orders (currently the only field that can be updated is fulfilled)

The service and DB layer are ready to add a wider functionality, but currently the API is limited to only
what is necessary.

```
PUT /orders/{id} UpdateOrderRequest

UpdateOrderRequest
    fulfilled: Option[Boolean]
```

```
curl -X PUT -d '{"fulfilled": true }' -H "Content-Type: application/json" http://localhost:8080/orders/3
```


We can simulate many concurrent incoming requests by running the following. It will run the curl command 100 times (max jobs in parallel can be changed by changing the -P param). Ideally I would have made a client to simulate this but I ran out of time :)

```
$ seq 1 100 | xargs -Iname -P 10 curl -X POST -d '{"orders": [ {"dishTypeName": "CHICKEN_NUGGETS", "tableNumber": 1} ] }' -H "Content-Type: application/json" http://localhost:8080/orders
```

### File Structure
Entry point: `src/main/scala/OrdererApp.scala`

```
.
├── index
├── src
| └── main
|   ├── resources
|   └── scala
|     ├── database
|     ├── handler
|     └── service
|       └── models
| └── test
|   └── scala
|       ├── database/daos
|       └── service
```