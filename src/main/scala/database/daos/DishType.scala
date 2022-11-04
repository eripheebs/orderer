package database.daos

import service.models.DishTypeName

final case class DishType(
    id: Int,
    typeName: service.models.DishTypeName,
    maxCookingTimeInMin: Int
)

// TO DO: dish type dB layer