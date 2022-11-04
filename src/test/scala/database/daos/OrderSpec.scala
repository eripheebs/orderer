package database.daos

import postgresProfile.MyPostgresProfile.api._
import database.daos.{Order, OrderDao, OrderFilters, OrderOverrides}
import java.time.LocalDateTime

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.SequentialNestedSuiteExecution
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import scala.concurrent.Future
import slick.jdbc.GetResult

// tests run in parallel by default, SequentialNestedSuiteExecution means these tests will be run sequentially.
class OrdersSpec extends AnyWordSpec with SequentialNestedSuiteExecution with BeforeAndAfterAll with Matchers with ScalaFutures {
    implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(5, Seconds))
    
    // To do: extract to setup file
    var db = Database.forConfig("postgresTest")

    def afterAll(configMap: Map[String, Any]) {
        db.close()
    }
    
    val fulfilledOrder: Order = Order(4, 3, "Chicken nuggets", LocalDateTime.of(2022, 11, 7, 10, 30), LocalDateTime.of(2022, 11, 7, 10, 30), 6, true, Some(LocalDateTime.of(2022, 11, 7, 10, 30)), false, None)
    val cancelledOrderTable5: Order = Order(2, 4, "Fries",  LocalDateTime.of(2022, 11, 7, 10, 30), LocalDateTime.of(2022, 11, 7, 10, 30), 5, false, Some(LocalDateTime.of(2022, 11, 7, 10, 30)), true, Some(LocalDateTime.of(2022, 11, 7, 10, 30)))
    val cancelledOrderTable6: Order = Order(3, 4, "Fries",  LocalDateTime.of(2022, 11, 7, 10, 30), LocalDateTime.of(2022, 11, 7, 10, 30), 6, false, Some(LocalDateTime.of(2022, 11, 7, 10, 30)), true, Some(LocalDateTime.of(2022, 11, 7, 10, 30)))

    // the interpolation is not working -.-
    // def orderToSqlInsertion (order: Order) : DBIO[Int] = sqlu"insert into orders values (${order.id}, ${order.dishTypeId}, '${order.dishTypeName}', '${order.orderTime.toString()}', '${order.expectedTime.toString()}', ${order.tableNumber}, ${order.fulfilled}, '${order.fulfilledTime.toString()}', ${order.cancelled}, '${order.cancelledTime.toString()}')"
    val fulfilledOrderSql = sqlu"insert into orders values(4, 3, 'Chicken nuggets', '2022-11-07 10:30:00', '2022-11-07 10:30:00', 6, true, '2022-11-07 10:30:00', false, null)"
    val cancelledOrderTable5Sql = sqlu"insert into orders values(2, 4, 'Fries', '2022-11-07 10:30:00', '2022-11-07 10:30:00', 5, false, '2022-11-07 10:30:00', true, '2022-11-07 10:30:00')"
    val cancelledOrderTable6Sql = sqlu"insert into orders values(3, 4, 'Fries', '2022-11-07 10:30:00', '2022-11-07 10:30:00', 6, false, '2022-11-07 10:30:00', true, '2022-11-07 10:30:00')"

    def orderToString (order: Order) : String = s"insert into orders values(${order.id}, ${order.dishTypeId}, '${order.dishTypeName}', '${order.orderTime.toString()}', '${order.expectedTime.toString()}', ${order.tableNumber}, ${order.fulfilled}, '${order.fulfilledTime.toString()}', ${order.cancelled}, '${order.cancelledTime.toString()}')"

    // TODO - fix the interpolation.
    // val inserts: Seq[DBIO[Int]] = Seq(
    //     fulfilledOrder,
    //     cancelledOrder
    // ).map(orderToSqlInsertion)

    // TODO: figure out how to use this with before/after
    def setUpOders(db: Database): OrderDao = {
        val orders = new OrderDao(db)

        orders.dropSchema.futureValue
        orders.createSchema.futureValue

        val seed = DBIO.seq(
            fulfilledOrderSql,
            cancelledOrderTable5Sql,
            cancelledOrderTable6Sql
        )
        db.run(seed).futureValue

        orders
    }

    // TODO: figure out how to use this with before/after
    // def dropOrders(orders: OrderDao) = {
    //     orders.dropSchema()
    // }

    "Orders" should {
        "read an order from orders table" in {
            val orders = setUpOders(db)

            noException should be thrownBy orders.read(fulfilledOrder.id).futureValue.get
            orders.read(fulfilledOrder.id).futureValue.get should have (
                'id (fulfilledOrder.id),
                'dishTypeId (fulfilledOrder.dishTypeId),
                'dishTypeName (fulfilledOrder.dishTypeName),
                'orderTime (fulfilledOrder.orderTime),
                'expectedTime (fulfilledOrder.expectedTime),
                'tableNumber (fulfilledOrder.tableNumber),
                'fulfilled (fulfilledOrder.fulfilled),
                'fulfilledTime (fulfilledOrder.fulfilledTime),
                'cancelled (fulfilledOrder.cancelled),
                'cancelledTime (fulfilledOrder.cancelledTime),
            )
            orders.dropSchema.futureValue
        }

        "read all orders" in {
            val orders = setUpOders(db)

            val filters = OrderFilters(None, None, None)

            noException should be thrownBy orders.readAll(filters).futureValue
            val allOrders = orders.readAll(filters).futureValue
            allOrders.size should equal(3)
            val orderIds = allOrders.map(order => order.id)
            orderIds should contain (fulfilledOrder.id)
            orderIds should contain (cancelledOrderTable5.id)
            orderIds should contain (cancelledOrderTable6.id)
            orders.dropSchema.futureValue
        }

        "read all fulfilled orders" in {
            val orders = setUpOders(db)

            val filters = OrderFilters(Some(true), None, None)

            noException should be thrownBy orders.readAll(filters).futureValue
            val allOrders = orders.readAll(filters).futureValue
            allOrders.size should equal(1)
            allOrders.head should have (
                'id (fulfilledOrder.id),
                'dishTypeId (fulfilledOrder.dishTypeId),
                'dishTypeName (fulfilledOrder.dishTypeName),
                'orderTime (fulfilledOrder.orderTime),
                'expectedTime (fulfilledOrder.expectedTime),
                'tableNumber (fulfilledOrder.tableNumber),
                'fulfilled (fulfilledOrder.fulfilled),
                'fulfilledTime (fulfilledOrder.fulfilledTime),
                'cancelled (fulfilledOrder.cancelled),
                'cancelledTime (fulfilledOrder.cancelledTime),
            )
            orders.dropSchema.futureValue
        }

        "read all cancelled orders" in {
            val orders = setUpOders(db)

            val filters = OrderFilters(None, Some(true), None)

            noException should be thrownBy orders.readAll(filters).futureValue
            val allOrders = orders.readAll(filters).futureValue
            allOrders.size should equal(2)
            val orderIds = allOrders.map(order => order.id)
            orderIds should contain (cancelledOrderTable5.id)
            orderIds should contain (cancelledOrderTable6.id)
            orders.dropSchema.futureValue
        }

        "read orders filtered by cancelled and table number" in {
            val orders = setUpOders(db)

            val filters = OrderFilters(None, Some(true), Some(cancelledOrderTable6.tableNumber))

            noException should be thrownBy orders.readAll(filters).futureValue
            val allOrders = orders.readAll(filters).futureValue
            allOrders.size should equal(1)
            allOrders.head should have (
                'id (cancelledOrderTable6.id),
                'dishTypeId (cancelledOrderTable6.dishTypeId),
                'dishTypeName (cancelledOrderTable6.dishTypeName),
                'orderTime (cancelledOrderTable6.orderTime),
                'expectedTime (cancelledOrderTable6.expectedTime),
                'tableNumber (cancelledOrderTable6.tableNumber),
                'fulfilled (cancelledOrderTable6.fulfilled),
                'fulfilledTime (cancelledOrderTable6.fulfilledTime),
                'cancelled (cancelledOrderTable6.cancelled),
                'cancelledTime (cancelledOrderTable6.cancelledTime),
            )
            orders.dropSchema.futureValue
        }

        "read all orders by table number" in {
            val orders = setUpOders(db)

            val filters = OrderFilters(None, None, Some(6))

            noException should be thrownBy orders.readAll(filters).futureValue
            val allOrders = orders.readAll(filters).futureValue
            allOrders.size should equal(2)
            val orderIds = allOrders.map(order => order.id)
            orderIds should contain (fulfilledOrder.id)
            orderIds should contain (cancelledOrderTable6.id)
            orders.dropSchema.futureValue
        }

        "insert an order" in {
            val orders = setUpOders(db)

            val newOrder = Order(
                1,
                4,
                "Fries", 
                LocalDateTime.of(2022, 12, 25, 12, 30),
                LocalDateTime.of(2022, 12, 25, 12, 40),
                9,
                false,
                None,
                false,
                None
            )

            noException should be thrownBy orders.insert(newOrder).futureValue

            orders.read(1).futureValue.get should have (
                'id (newOrder.id),
                'dishTypeId (newOrder.dishTypeId),
                'dishTypeName (newOrder.dishTypeName),
                'orderTime (newOrder.orderTime),
                'expectedTime (newOrder.expectedTime),
                'tableNumber (newOrder.tableNumber),
                'fulfilled (newOrder.fulfilled),
                'fulfilledTime (newOrder.fulfilledTime),
                'cancelled (newOrder.cancelled),
                'cancelledTime (newOrder.cancelledTime),
            )
            orders.dropSchema.futureValue
        }

        "throw error if trying to update an order that doesnt exist" in {
            val orders = setUpOders(db)

            the [Exception] thrownBy {
                orders.update(99, OrderOverrides(None,None,None,None)).futureValue
                // TODO fix this so it doesnt have ot show the future part of the error.
            } should have message "The future returned an exception of type: java.lang.Exception, with message: Attempting to update Order that does not exist."
        }

        "update an order" in {
            val orders = setUpOders(db)

            val cancelledVal = true

            val overrides = OrderOverrides(
                None,
                None,
                Some(cancelledVal),
                Some(LocalDateTime.of(2022, 12, 25, 12, 40))
            )

            noException should be thrownBy orders.update(fulfilledOrder.id, overrides).futureValue

            val updated = orders.read(fulfilledOrder.id).futureValue.get
            orders.read(fulfilledOrder.id).futureValue.get should have (
                'cancelled (cancelledVal),
                'cancelledTime (overrides.cancelledTime),
            )
            orders.dropSchema.futureValue
        }

        "delete an order" in {
            val orders = setUpOders(db)

            noException should be thrownBy orders.delete(fulfilledOrder.id).futureValue

            orders.read(fulfilledOrder.id).futureValue should equal (None)
  
            orders.dropSchema.futureValue
        }
    }
}