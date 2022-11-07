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
import database.daos.{Order => DbOrder}

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
    }
}