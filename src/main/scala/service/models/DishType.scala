package service.models

final case class DishType(
    id: Long,
    typeName: service.models.DishTypeName,
    maxCookingTimeInMin: Int
)

class DishTypeName extends Enumeration {
    type DishTypeName = Value
    val ChickenNuggets = Value("CHICKEN_NUGGETS")
    val Fries = Value("FRIES")
}


// get name from Typename function.