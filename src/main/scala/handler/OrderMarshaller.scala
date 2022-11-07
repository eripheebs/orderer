package handler

import play.api.libs.json.{Json, OFormat}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

import service.models.{Order, MakeOrderReq => ServiceMakeOrderReq}

// Order marhsaller converts the Order model to and from json.
trait OrderMarshaller extends SprayJsonSupport with DefaultJsonProtocol {
	implicit val orderFormat: OFormat[Order] = Json.format[Order]
	implicit val serviceOrderRequestFormat = jsonFormat2(ServiceMakeOrderReq.apply)
	implicit val makeOrderRequestFormat = jsonFormat1(MakeOrdersRequest.apply)
	implicit val makeOrderResponseFormat = jsonFormat1(MakeOrdersResponse.apply)
}

object OrderMarshaller extends OrderMarshaller