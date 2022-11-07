package handler

import akka.actor._
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import scala.util.{ Failure, Success }

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes.{BadRequest, Created}
// import akka.http.scaladsl.model.HttpResponse

// import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

import service.models.{Order, MakeOrderReq => ServiceMakeOrderReq}
import orderer.HandlerDependencies

case class MakeOrdersResponse(ids: Seq[Int])

case class MakeOrdersRequest(orders: Seq[ServiceMakeOrderReq])

class OrderHandler(deps: HandlerDependencies) extends OrderMarshaller {
	// ROUTES
	val createOrders = "orders"

	protected val orderRoutes: Route = {
		concat(
		// POST /orders
			post {
				entity(as[MakeOrdersRequest]) { params =>
					val ordersFuture: Future[Seq[Int]] = deps.ordersService.makeOrders(
						params.orders
					)
					onComplete(ordersFuture) {
						case Success(ids) => complete(Created, MakeOrdersResponse(ids))
						// Here error types could be broken down and specific user friendly errors returned
						case Failure(ex) => complete(BadRequest)
					}
				}
			}
		)
  }

  
  val routes: Route = orderRoutes
}
