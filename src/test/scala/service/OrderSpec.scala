package service

import org.scalatest.wordspec.AnyWordSpec
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import java.time.LocalDateTime
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.Future

import database.daos.OrderDao
import orderer.OrderServiceDependencies
import service.models.{MakeOrderReq, DishTypeName}
import database.daos.{Order => DbOrder, OrderOverrides => DbOrderOverrides, OrderFilters => DbOrderFilters}

class TestClock extends Clock {
    def now(): LocalDateTime = LocalDateTime.of(2022, 11, 7, 10, 30)
}

class OrdersSpec extends AnyWordSpec with Matchers with MockFactory with ScalaFutures{
    val mockDb = mock[OrderDao]
    val deps = OrderServiceDependencies(mockDb, new TestClock)
    val orders = new OrderService(deps)

    val order1 = DbOrder(1, DishTypeName.ChickenNuggets, LocalDateTime.of(2022, 11, 7, 10, 30), LocalDateTime.of(2022, 11, 7, 10, 30), 1, false, None, false, None)

    "Orders service" should {
        "make an order" in {
            val req = MakeOrderReq(
                DishTypeName.ChickenNuggets,
                1
            )
            val dbOrder = DbOrder(
                1,
                req.dishTypeName,
                LocalDateTime.of(2022, 11, 7, 10, 30),
                LocalDateTime.of(2022, 11, 7, 11, 0),
                req.tableNumber,
                false,
                None,
                false,
                None
            )

            (mockDb.insertAll _).expects(Seq(dbOrder)).returning(Future.successful((Seq(1))))

            val ids = orders.makeOrders(Seq(req))
            ids.futureValue should equal(Seq(1))
        }

        "make multiple orders" in {
            val req1 = MakeOrderReq(
                DishTypeName.ChickenNuggets,
                1
            )
            val req2 = MakeOrderReq(
                DishTypeName.Fries,
                2
            )

            val dbOrder1 = DbOrder(
                1,
                req1.dishTypeName,
                LocalDateTime.of(2022, 11, 7, 10, 30),
                LocalDateTime.of(2022, 11, 7, 11, 0),
                req1.tableNumber,
                false,
                None,
                false,
                None
            )

            val dbOrder2 = DbOrder(
                1,
                req2.dishTypeName,
                LocalDateTime.of(2022, 11, 7, 10, 30),
                LocalDateTime.of(2022, 11, 7, 11, 0),
                req2.tableNumber,
                false,
                None,
                false,
                None
            )

            (mockDb.insertAll _).expects(Seq(dbOrder1, dbOrder2)).returning(Future.successful((Seq(1, 2))))

            val ids = orders.makeOrders(Seq(req1, req2))
            ids.futureValue should equal(Seq(1, 2))
        }

        "fulfill an order" in {
            val id = 3
            val overrides = DbOrderOverrides(
                Some(true),
                Some(LocalDateTime.of(2022, 11, 7, 10, 30)),
                None,
                None
            )
            (mockDb.update _).expects(id, overrides).returning(Future.successful(1))

            val returnedId = orders.fulfillOrder(id)
            returnedId.futureValue should equal(1)
        }

        "should get orders by table number" in {
            val tableNumber = 1

            val filters = DbOrderFilters(
                Some(false),
                Some(false),
                Some(tableNumber)
            )

            val dbOrder = DbOrder(
                1,
                DishTypeName.ChickenNuggets,
                LocalDateTime.of(2022, 11, 7, 10, 30),
                LocalDateTime.of(2022, 11, 7, 11, 0),
                tableNumber,
                false,
                None,
                false,
                None
            )

            (mockDb.readAll _).expects(filters).returning(Future.successful(Seq(dbOrder)))

            val returnedOrders = orders.getOrdersByTableNumber(tableNumber).futureValue
            returnedOrders.head should have (
                'id (dbOrder.id),
                'dishTypeName (dbOrder.dishTypeName),
                'orderTime (dbOrder.orderTime),
                'expectedTime (dbOrder.expectedTime),
                'tableNumber (dbOrder.tableNumber),
                'fulfilled (dbOrder.fulfilled),
                'fulfilledTime (dbOrder.fulfilledTime),
                'cancelled (dbOrder.cancelled),
                'cancelledTime (dbOrder.cancelledTime),
            )
        }

        "should get order by id" in {
            val id = 1

            val dbOrder = DbOrder(
                1,
                DishTypeName.ChickenNuggets,
                LocalDateTime.of(2022, 11, 7, 10, 30),
                LocalDateTime.of(2022, 11, 7, 11, 0),
                4,
                false,
                None,
                false,
                None
            )

            (mockDb.read _).expects(id).returning(Future.successful(Some(dbOrder)))

            val returnedOrder = orders.getOrdersById(id).futureValue
            returnedOrder should have (
                'id (dbOrder.id),
                'dishTypeName (dbOrder.dishTypeName),
                'orderTime (dbOrder.orderTime),
                'expectedTime (dbOrder.expectedTime),
                'tableNumber (dbOrder.tableNumber),
                'fulfilled (dbOrder.fulfilled),
                'fulfilledTime (dbOrder.fulfilledTime),
                'cancelled (dbOrder.cancelled),
                'cancelledTime (dbOrder.cancelledTime),
            )
        }

        "should throw exception if order by id returns None" in {
            val id = 1

            val dbOrder = DbOrder(
                1,
                DishTypeName.ChickenNuggets,
                LocalDateTime.of(2022, 11, 7, 10, 30),
                LocalDateTime.of(2022, 11, 7, 11, 0),
                4,
                false,
                None,
                false,
                None
            )

            (mockDb.read _).expects(id).returning(Future.successful(None))

             the [Exception] thrownBy {
                orders.getOrdersById(id).futureValue
                // TODO fix this so it doesnt have ot show the future part of the error.
            } should have message "The future returned an exception of type: java.lang.Exception, with message: Attempting to get order that does not exist."
        }
    }
}