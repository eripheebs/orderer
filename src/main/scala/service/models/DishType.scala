package service.models

import java.time.Duration

object DishTypeName extends Enumeration {
	type DishTypeName = Value
	val ChickenNuggets = "CHICKEN_NUGGETS"
	val Fries = "FRIES"
}

final case class DishType(
	typeName: String,
	maxCookingTimeInMin: Int
)

