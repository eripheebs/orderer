package service.models
import java.time.LocalDate

final case class Order(
	id: Long,
	// dishTypeName: Dish types are the type of dish ordered e.g. "CHICKEN_NUGGETS", we can get
	// the full name of the dish from the type.
	dishTypeName: String, 
	// orderTime: time of Order
	orderTime: LocalDate,
	// expectedTime: time the order is expected by AKA when it was promised to be delivered by
	// to the customer. This is calculated at order creation by OrderTime + DishType.maxCookingTimeInMin
	expectedTime: LocalDate,
	// tableNumber: Ideally we'd represent table numbers in a way that we could easily change
	// the numbers, an enum or it's own table where you could update other details
	// but I'll just use int for now to keep it simple.
	tableNumber: Int,
	// fulfilled: whether or not the order was fulfilled
	fulfilled: Boolean,
	fulfilledTime: LocalDate,
	// cancelled: if the order was cancelled before it was fulfilled.
	cancelled: Boolean,
	cancelledtime: LocalDate
)

final case class MakeOrderReq(
	dishTypeName: String,
	tableNumber: Int
)