package handler

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.{Actor, Props}
import akka.actor.typed.scaladsl.Behaviors
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import scala.util.{ Failure, Success }

import scala.language.postfixOps
// TODO remove wildcard imports in favor of specific imports.
import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask
import scala.concurrent.Future
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes.{NotFound, OK, BadRequest, Created}
import service.models.{MakeOrderReq => ServiceMakeOrderReq}

import service.models.{Order, MakeOrderReq => ServiceMakeOrderReq}
import orderer.HandlerDependencies
import scala.concurrent.duration.Duration

// Request and Response classes
case class MakeOrdersRequest(orders: Seq[ServiceMakeOrderReq])
case class UpdateOrderRequest(fulfilled: Option[Boolean])

case class MakeOrdersResponse(ids: Seq[Int])
case class GetOrderByIdResponse(order: Order)
case class GetOrdersByTableNumberResponse(orders: Seq[Order])
case class FulfillOrderByIdResponse(id: Int)

// Actor Messages
final case class MakeOrdersMessage(payload: Seq[ServiceMakeOrderReq])
final case class GetOrderByIdMessage(id: Int)
final case class GetOrdersByTableNumberMessage(id: Int)
final case class FulfillOrderByIdMessage(id: Int)

// Akka actor (https://doc.akka.io/docs/akka/2.2/scala/actors.html)
// Actors have unique addresses & mailboxes and messages will remain in
// the mailboxes until the actor is ready to process it. When an actor is ready
// it will pick one message at a time and execute it's behaviour.
class OrderActor(deps: HandlerDependencies) extends Actor {
	implicit val ec = deps.system.dispatcher
	implicit val timeout: Timeout = 20 seconds

	def receive = {
		case MakeOrdersMessage(payload) => deps.ordersService.makeOrders(
				payload
			) pipeTo sender()
		case GetOrderByIdMessage(id) => deps.ordersService.getOrderById(
				id
			) pipeTo sender()
		case GetOrdersByTableNumberMessage(tableNumber) => deps.ordersService.getOrdersByTableNumber(
				tableNumber
			) pipeTo sender()
		case FulfillOrderByIdMessage(id) => deps.ordersService.fulfillOrder(id) pipeTo sender()
		case _ => throw new Exception("Unknown Message sent to OrderActor")
	}
}

class OrderHandler(deps: HandlerDependencies) extends OrderMarshaller {
	val orderActor = deps.system.actorOf(Props.create(classOf[OrderActor], deps))

	// Todo pass timeout through deps.
	implicit val timeout: Timeout = 20 seconds

	// ROUTES
	val ordersPath = "orders"

	protected val orderRoutes: Route = {
		concat(
			pathPrefix(ordersPath) {
				concat(
					// POST /orders
					post {
						entity(as[MakeOrdersRequest]) { params =>
							val ordersFuture: Future[Seq[Int]] = ask(orderActor, MakeOrdersMessage(params.orders)).mapTo[Seq[Int]]
							onComplete(ordersFuture) {
								case Success(ids) => complete(Created, MakeOrdersResponse(ids.asInstanceOf[Seq[Int]]))
								// Here error types could be broken down and specific user friendly errors returned
								case Failure(ex) => complete(BadRequest)
							}
						}
					},
					// GET /orders/{id}
					(get & path(LongNumber)) {
						id =>
							val orderFuture: Future[Order] = ask(orderActor, GetOrderByIdMessage(id.toInt)).mapTo[Order]
							onComplete(orderFuture) {
								case Success(order) => complete(OK, GetOrderByIdResponse(order.asInstanceOf[Order]))
								case Failure(ex) => {
									if (ex.getMessage() == "Attempting to get order that does not exist") {
										complete(NotFound)
									} else {
										complete(BadRequest)
									}
								}
							}
					},
					// GET /orders/by-table-number/{tableNumber}
					(get & path("by-table-number" / LongNumber)) {
						tableNumber =>
							val ordersFuture: Future[Seq[Order]] = ask(orderActor, GetOrdersByTableNumberMessage(tableNumber.toInt)).mapTo[Seq[Order]]
							onComplete(ordersFuture) {
								case Success(order) => complete(OK, GetOrdersByTableNumberResponse(order.asInstanceOf[Seq[Order]]))
								case Failure(ex) => complete(BadRequest)
							}
					},
					// PUT /orders/{id}
					// Add validation so that it errors when unexpected fields are passed to put /orders/id.
					(put & path(LongNumber)) {
						id => entity(as[UpdateOrderRequest]) { params => params.fulfilled match {
							case Some(true) => {
								val idFuture: Future[Int] = ask(orderActor, FulfillOrderByIdMessage(id.toInt)).mapTo[Int]
								println(idFuture.value)
								onComplete(idFuture) {
									case Success(iD) => complete(OK, FulfillOrderByIdResponse(id.asInstanceOf[Int]))
									case Failure(ex) => {
										if (ex.getMessage() == "Attempting to update Order that does not exist") {
											complete(NotFound)
										} else {
											complete(BadRequest)
										}
									}
								}
							}
							// This can be fleshed out for other updates like cancelled
							case _ => complete(OK)
						}}
					}
				)
			}
		)
  }

  
  val routes: Route = orderRoutes
}
