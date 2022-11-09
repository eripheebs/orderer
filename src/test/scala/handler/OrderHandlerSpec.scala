package handler

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.BeforeAndAfterAll
import org.scalamock.scalatest.MockFactory
import java.time.LocalDateTime
import org.scalatest.concurrent.ScalaFutures
import akka.actor.ActorSystem
import scala.concurrent.Future
import akka.http.scaladsl.server._
import Directives._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import akka.http.scaladsl.model.{HttpEntity, HttpMethods, HttpRequest, ContentTypes}
import scala.concurrent.duration._
import akka.testkit.TestKit
import akka.http.scaladsl.testkit.RouteTestTimeout

import orderer.HandlerDependencies
import service.OrderService
import service.models.{Order, MakeOrderReq => ServiceMakeOrderReq}
import service.models.DishTypeName

class OrderHandlerSpec extends AnyWordSpec with Matchers with ScalatestRouteTest with MockFactory with BeforeAndAfterAll with ScalaFutures {
	implicit def default(implicit system: ActorSystem) = RouteTestTimeout(5.second)

	val mockOrderService = mock[OrderService]
	val deps = HandlerDependencies(mockOrderService, system)
	val handler = new OrderHandler(deps)
	val routes = handler.routes
	val pretendOrder = Order(1, DishTypeName.ChickenNuggets, LocalDateTime.of(2022, 11, 7, 10, 30), LocalDateTime.of(2022, 11, 7, 10, 30), 1, false, None, false, None)
	val pretendOrder2 = Order(1, DishTypeName.Fries, LocalDateTime.of(2022, 11, 7, 10, 30), LocalDateTime.of(2022, 11, 7, 10, 30), 5, false, None, false, None)

	override def afterAll(): Unit = {
		TestKit.shutdownActorSystem(system)
	}

	"OrderHandler" should {
		"return an order when making an Order Request" in {
			val serviceMakeOrderReq = ServiceMakeOrderReq("CHICKEN_NUGGETS", 1)
			(mockOrderService.makeOrders _).expects(Seq(serviceMakeOrderReq)).returning(Future.successful((Seq(1))))

			Post("/orders").withEntity(HttpEntity(
				ContentTypes.`application/json`,
				"""{"orders": [ {"dishTypeName": "CHICKEN_NUGGETS", "tableNumber": 1} ] }""")
			) ~> routes ~> check {

				responseAs[String] shouldEqual """{"ids":[1]}"""
			}
		}

		"return an order by ID" in {
			(mockOrderService.getOrderById _).expects(1).returning(Future.successful(pretendOrder))

			Get("/orders/1") ~> routes ~> check {

				responseAs[String] shouldEqual """{"order":{"cancelled":false,"dishTypeName":"CHICKEN_NUGGETS","expectedTime":"2022-11-07T10:30:00","fulfilled":false,"id":1,"orderTime":"2022-11-07T10:30:00","tableNumber":1}}"""
			}
		}

		"return orders by table number" in {
			(mockOrderService.getOrdersByTableNumber _).expects(1).returning(Future.successful((Seq(pretendOrder, pretendOrder2))))

			Get("/orders/by-table-number/1") ~> routes ~> check {

				responseAs[String] shouldEqual """{"orders":[{"cancelled":false,"dishTypeName":"CHICKEN_NUGGETS","expectedTime":"2022-11-07T10:30:00","fulfilled":false,"id":1,"orderTime":"2022-11-07T10:30:00","tableNumber":1},{"cancelled":false,"dishTypeName":"FRIES","expectedTime":"2022-11-07T10:30:00","fulfilled":false,"id":1,"orderTime":"2022-11-07T10:30:00","tableNumber":5}]}"""
			}
		}

		"update order to fulfilled" in {
			(mockOrderService.fulfillOrder _).expects(1).returning(Future.successful(1))

			Put("/orders/1").withEntity(HttpEntity(
				ContentTypes.`application/json`,
				"""{"fulfilled": true }""")
			) ~> routes ~> check {

				responseAs[String] shouldEqual """{"id":1}"""
			}
		}

		"leave GET requests to non defined paths unhandled" in {
			Get("/order") ~> routes ~> check {
				handled shouldBe false
			}
		}
	}
}
