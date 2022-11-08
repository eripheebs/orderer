package service

import scala.concurrent.Future
import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global

import orderer.OrderServiceDependencies
import database.daos.{Order => DbOrder, OrderOverrides => DbOrderOverrides, OrderFilters => DbOrderFilters}
import service.models.{DishType, DishTypeName, MakeOrderReq, Order}

class OrderService(deps: OrderServiceDependencies) {
    def makeOrders(requests: Seq[MakeOrderReq]): Future[Seq[Int]] = deps.db.insertAll(requests.map(req => requestToDbOrder(req)))

    def fulfillOrder(id: Int): Future[Int] = deps.db.update(id, DbOrderOverrides(
        Some(true),
        Some(deps.clock.now()),
        None,
        None
    ))

    def getOrdersByTableNumber(tableNum: Int): Future[Seq[Order]] = deps.db.readAll(DbOrderFilters(
        Some(false),
        Some(false),
        Some(tableNum)
    )).flatMap(orders => Future.successful(orders.map(order => Order(order.id,
        order.dishTypeName,
        order.orderTime,
        order.expectedTime,
        order.tableNumber,
        order.fulfilled,
        order.fulfilledTime,
        order.cancelled,
        order.cancelledTime
    ))))

    def getOrderById(id: Int): Future[Order] = deps.db.read(id).flatMap(order => order match {
        case Some(order) => Future.successful(Order(order.id,
            order.dishTypeName,
            order.orderTime,
            order.expectedTime,
            order.tableNumber,
            order.fulfilled,
            order.fulfilledTime,
            order.cancelled,
            order.cancelledTime))
        case None => throw new Exception("Attempting to get order that does not exist")
    })

    private def requestToDbOrder(request: MakeOrderReq): DbOrder = {
        val dishType = getDishTypeFromName(request.dishTypeName)

        val timeNow = deps.clock.now()
        val expectedTime = timeNow.plusMinutes(dishType.maxCookingTimeInMin)

        return DbOrder(
            // ideally i'd have an OrderInsert class that omits the ID, but to save time I do not
            // right now so just stub with a 1, it will be ignored upon insert.
            1,
            request.dishTypeName,
            timeNow,
            expectedTime,
            request.tableNumber,
            false,
            None,
            false,
            None
        )
    }

    // This would in theory read from a dish type service (deps.dishTypes) which would return
    // the dish type info
    // but to save time we're just gonna do it all here and always make it expected in 30 minutes.
    private def getDishTypeFromName(name: String): DishType = name match {
        case DishTypeName.ChickenNuggets => DishType(
            DishTypeName.ChickenNuggets,
            30
        )
        case DishTypeName.Fries => DishType(
            DishTypeName.Fries,
            30
        )
        case _ => throw new Exception("Dish type is not supported.")
    }
}