package database.daos

import java.time.LocalDateTime
import postgresProfile.MyPostgresProfile.api._
import scala.concurrent.Future
import database.Dao
import slick.sql.FixedSqlAction
import scala.concurrent.ExecutionContext.Implicits.global
import slick.sql.SqlProfile.ColumnOption.SqlType

final case class Order(
	// usually I'd had a separate types called OrderInsert from Order to make
	// it clear which values are required for insertion (e.g. ID is not required as it is assigned in this layer)
	// but to save time I'll use the same type
	id: Int,
	// The dish's identifier.
	dishTypeName: String, 
	orderTime: LocalDateTime,
	expectedTime: LocalDateTime,
	// Ideally we'd represent table numbers in a way that we could easily change
	// the numbers and it wouldnt matter but for simplicity will just store int.
	tableNumber: Int,
	fulfilled: Boolean,
	fulfilledTime: Option[LocalDateTime],
	cancelled: Boolean,
	cancelledTime: Option[LocalDateTime]
)

final case class OrderFilters (
	fulfilledFilterOn: Option[Boolean],
	cancelledFilterOn: Option[Boolean],
	tableNumber: Option[Int]
)

// The properties of Order that can be updated
final case class OrderOverrides(
	fulfilled: Option[Boolean],
	fulfilledTime: Option[LocalDateTime],
	cancelled: Option[Boolean],
	cancelledTime: Option[LocalDateTime]
)

class OrderDao(db: Database) extends Dao[Order, OrderOverrides, OrderFilters] {
	class OrderTable(tag: Tag) extends Table[Order](tag, "orders") {
		def id = column[Int]("id", SqlType("SERIAL"), O.PrimaryKey, O.AutoInc)
		def dishTypeName = column[String]("dish_type_name")
		def orderTime = column[LocalDateTime]("order_time")
		def expectedTime = column[LocalDateTime]("expected_time")
		def tableNumber = column[Int]("table_number")
		def fulfilled = column[Boolean]("fulfilled")
		def fulfilledTime = column[Option[LocalDateTime]]("fulfilled_time")
		def cancelled = column[Boolean]("cancelled")
		def cancelledTime = column[Option[LocalDateTime]]("cancelled_time")
		override def * = (id, dishTypeName, orderTime, expectedTime, tableNumber, fulfilled, fulfilledTime, cancelled, cancelledTime, 
		) <> (Order.tupled, Order.unapply)
	}

	val orderTable = TableQuery[OrderTable]
	
	override def createSchema(): Future[Unit] = db.run(orderTable.schema.createIfNotExists)

	override def insert(data: Order): Future[Int] = db.run((orderTable returning orderTable.map(_.id)) += data)

	override def insertAll(items: Seq[Order]): Future[Seq[Int]] = {
		val query = DBIO.sequence(items.map(order => orderTable returning orderTable.map(_.id) += order))
		db.run(query.transactionally)
	}

	override def read(id: Int): Future[Option[Order]] = db.run(orderTable.filter(_.id === id).take(1).result.headOption)

	override def readAll(filters: OrderFilters): Future[Seq[Order]] = {
		val query = orderTable
			.filterOpt(filters.fulfilledFilterOn){ case (order, _) => order.fulfilled === filters.fulfilledFilterOn.get }
			.filterOpt(filters.cancelledFilterOn){ case (order, _) => order.cancelled === filters.cancelledFilterOn.get }
			.filterOpt(filters.tableNumber){ case (order, _) => order.tableNumber === filters.tableNumber.get }
			.result

		db.run(query)
	}

	override def update(id: Int, row: OrderOverrides): Future[Int] = {
		read(id).flatMap {
			case Some(existingOrder) =>
				db.run(orderTable.filter(_.id === id)
					.map(order => (order.fulfilled, order.fulfilledTime, order.cancelled, order.cancelledTime))
					.update((row.fulfilled.getOrElse(existingOrder.fulfilled),
						row.fulfilledTime.orElse(existingOrder.fulfilledTime),
						row.cancelled.getOrElse(existingOrder.cancelled),
						row.cancelledTime.orElse(existingOrder.cancelledTime))))
			// TODO: Typed Exceptions.
			case None => throw new Exception("Attempting to update Order that does not exist")
		}
	}

	// for unit tests
	def dropSchema(): Future[Unit] = db.run(orderTable.schema.dropIfExists)
}
