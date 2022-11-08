package handler

import play.api.libs.json.{Json, OFormat}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import service.models.{Order, MakeOrderReq => ServiceMakeOrderReq}

final case class SomeData (dateTime: LocalDateTime)

// Order marhsaller converts the Order models to and from json.
trait OrderMarshaller extends SprayJsonSupport with DefaultJsonProtocol {
	implicit val eventDataFormat: JsonFormat[SomeData] = jsonFormat1(SomeData)

	implicit val localDateTimeFormat = new JsonFormat[LocalDateTime] {
		private val iso_date_time = DateTimeFormatter.ISO_DATE_TIME
		def write(x: LocalDateTime) = JsString(iso_date_time.format(x))
		def read(value: JsValue) = value match {
			case JsString(x) => LocalDateTime.parse(x, iso_date_time)
			case x => throw new RuntimeException(s"Unexpected type ${x.getClass.getName} when trying to parse LocalDateTime")
		}
	}
	
	implicit val serviceOrderRequestFormat = jsonFormat2(ServiceMakeOrderReq.apply)
	implicit val makeOrderRequestFormat = jsonFormat1(MakeOrdersRequest.apply)
	implicit val makeOrderResponseFormat = jsonFormat1(MakeOrdersResponse.apply)
	implicit val orderFormat = jsonFormat9(Order.apply)
	implicit val getOrderByIdResponseFormat = jsonFormat1(GetOrderByIdResponse.apply)
	implicit val getOrdersByTableNumberResponseFormat = jsonFormat1(GetOrdersByTableNumberResponse.apply)
	implicit val fulfilledOrdersByIdResponseFormat = jsonFormat1(FulfillOrderByIdResponse.apply)
	implicit val updateOrderRequestFormat = jsonFormat1(UpdateOrderRequest.apply)
}

object OrderMarshaller extends OrderMarshaller