package database

import scala.concurrent.Future

// trait for each data access model and basic CRUD functionality
// using generics here so it can be used across dao types
trait Dao[T, O, F] {
	def createSchema(): Future[Unit]

	// insert should return the ID
	// using the dao type as the parameter to save time
	def insert(data: T): Future[Int]

	def insertAll(items: Seq[T]): Future[Seq[Int]]

	// update should return the ID
	// using the dao type as the parameter for overrides to save time
	def update(id: Int, overrides: O): Future[Int]

	// read should return the model or nil if it does not exist
	def read(id: Int): Future[Option[T]]

	// readAll should return all 
	// we could add a date range and sort here too,
	// but it's out of scope for now.
	// filter for filtering by e.g. cancelled, fulfilled etc.
	// filter type should be defined in the dao definition.
	def readAll(filters: F): Future[Seq[T]]
}
