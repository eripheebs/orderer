package orderer

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import postgresProfile.MyPostgresProfile.api._
import database.daos.OrderDao
import service.OrderService
import service.{Clock, DefaultClock}

// todo: extract to own file
// todo: add config
final case class AppConfig ()

final case class Services (
    orders: OrderService
)

// Dependencies to be passed to the order service.
// This could inherit from a ServiceDependencies class that
// has Logging, Context, ETC.
final case class OrderServiceDependencies (
    // DB for Orders would only ever talk to the Order Service.
    db: OrderDao,
    clock: Clock
    // Other services could be passed here.
    // E.g. the DishType service.
)

// can pass config in here if we want to change the DB setup etc
// no config is set up to be passed ATM
class OrdererApp(config: AppConfig) {
    // here we could initialise logging etc and pass it down to the different layers 

    // SET UP DB LAYER

    // todo make this more configurable
    val db = Database.forConfig("postgres")
    
    val ordersDb = new OrderDao(db)
    Await.result(ordersDb.createSchema, Duration.Inf)

    val serviceDeps = OrderServiceDependencies(
        ordersDb,
        new DefaultClock
    )

    
    // pass the DB to the create schema initialiser

    // initialise service layer. pass Dependencies
    // set up handlers. pass Dependencies

    // close DB
}
