package orderer

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import akka.stream.ActorMaterializer
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.LoggerOps
import akka.actor.ActorSystem
import akka.actor.typed.{ ActorRef, Behavior }
import scala.concurrent.ExecutionContextExecutor

import postgresProfile.MyPostgresProfile.api._
import database.daos.OrderDao
import service.OrderService
import service.{Clock, DefaultClock}
import handler.OrderHandler
import scala.concurrent.Future
import akka.http.scaladsl.Http
import scala.io.StdIn
import com.typesafe.config._

// todo: extract Config class
// make logging, actor system behaviour etc all configurable
// & seperate from the libraries. 
final case class AppConfig() {
	val env = if (System.getenv("APP_ENV") == null) "development" else System.getenv("APP_ENV")

	val conf = ConfigFactory.load()
	def getConfig() = conf.getConfig(env)
}

final case class Services (
	orders: OrderService
)

// Dependencies to be injected to the order service.
// This could inherit from a ServiceDependencies class that
// has Logging, Context, ETC.
final case class OrderServiceDependencies (
	// DB for Orders would only ever talk to the Order Service.
	db: OrderDao,
	clock: Clock
	// Other services could be passed here.
	// E.g. the DishType service.
)

final case class HandlerDependencies(
	ordersService: OrderService,
	system: ActorSystem
	// pass any config, logging etc here
)

object OrdererApp extends App {
	// here we could initialise logging etc and pass it down to the different layers 

	// SET UP DB LAYER

	val config = new AppConfig()

	// todo make this more configurable
	// TODO use ENV for db config resolution.
	val dbConfigName = config.getConfig().getString("dbConfig")
	val db = Database.forConfig(dbConfigName)
	
	val ordersDb = new OrderDao(db)
	Await.result(ordersDb.createSchema, Duration.Inf)

	val serviceDeps = OrderServiceDependencies(
		ordersDb,
		new DefaultClock
	)

	val orderService = new OrderService(serviceDeps)

	// TODO use ENV for config
	val host = "localhost"
	val port = 8080

	// TODO add config to application.conf
	// Create the Akka actor system.
	implicit val system = ActorSystem()
	// The ExecutionContext is used to configure how and on which
	// thread pools Futures will run,so we'll pass down the dispatcher
	// if we need to declare the execution policy elsewhere in the app.
	implicit val ec: ExecutionContextExecutor = system.dispatcher
	// Actors have unique addresses & mailboxes and messages will remain in
	// the mailboxes until the actor is ready to process it. When an actor is ready
	// it will pick one message at a time and execute it's behaviour.
	// The ActorMaterializer creates actors
	implicit val materializer : ActorMaterializer = ActorMaterializer()

	val handlerDeps = new HandlerDependencies(orderService, system)
	val handler = new OrderHandler(handlerDeps)

	val api = handler.routes
	val bindingFuture: Future[Http.ServerBinding] = Http().bindAndHandle(api, host, port)

	try {
		bindingFuture.map { serverBinding =>
			println(s"Running on ${serverBinding.localAddress}")
		}
	}	catch {
		case ex: Exception => println("Server binding failed")
		system.terminate()
	}

	StdIn.readLine() // let it run until user presses return
	bindingFuture
		.flatMap(_.unbind()) // trigger unbinding from the port
		.onComplete(_ => {
			db.close()
			system.terminate()
		}) // and shutdown when done
}
