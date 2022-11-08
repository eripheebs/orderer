package handler

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.{Actor, Props}
import akka.actor.typed.scaladsl.Behaviors
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import scala.util.{ Failure, Success }

import scala.language.postfixOps
import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes.{BadRequest, Created}
import service.models.{MakeOrderReq => ServiceMakeOrderReq}

import service.models.{Order, MakeOrderReq => ServiceMakeOrderReq}
import orderer.HandlerDependencies
import scala.concurrent.duration.Duration


case class MakeOrdersResponse(ids: Seq[Int])

case class MakeOrdersRequest(orders: Seq[ServiceMakeOrderReq])

final case class MakeOrdersMessage(payload: Seq[ServiceMakeOrderReq])

// Akka actor (https://doc.akka.io/docs/akka/2.2/scala/actors.html)
// Actors have unique addresses & mailboxes and messages will remain in
// the mailboxes until the actor is ready to process it. When an actor is ready
// it will pick one message at a time and execute it's behaviour.
class OrderActor(deps: HandlerDependencies) extends Actor {
	implicit val ec = context.dispatcher
	implicit val timeout: Timeout = 20 seconds

	def receive = {
		case MakeOrdersMessage(payload) => {
			val senderName = deps.ordersService.makeOrders(
				payload
			) pipeTo sender()
		}
		case _ => throw new Exception("Unknown Message sent to OrderActor")
	}
}

class OrderHandler(deps: HandlerDependencies) extends OrderMarshaller {
	val orderActor = deps.system.actorOf(Props.create(classOf[OrderActor], deps))

	// Todo pass timeout through deps.
	implicit val timeout: Timeout = 20 seconds

	// ROUTES
	val createOrders = "orders"

	protected val orderRoutes: Route = {
		concat(
		// POST /orders
			path(createOrders) {
				post {
					entity(as[MakeOrdersRequest]) { params =>
						val ordersFuture: Future[Seq[Int]] = ask(orderActor, MakeOrdersMessage(params.orders)).mapTo[Seq[Int]]
						onComplete(ordersFuture) {
							case Success(ids) => complete(Created, MakeOrdersResponse(ids.asInstanceOf[Seq[Int]]))
							// Here error types could be broken down and specific user friendly errors returned
							case Failure(ex) => complete(BadRequest)
						}
					}
				}
			}
		)
  }

  
  val routes: Route = orderRoutes
}
