package dev.zio_rest_api

import zio.json.JsonEncoder
import zio.json.DeriveJsonEncoder
import zio.ZIOAppDefault

case class Mushroom(name: String)
object Mushroom {
  implicit val encoder: JsonEncoder[Mushroom] =
    DeriveJsonEncoder.gen[Mushroom]
}

case class Rows(
    Rows: List[Map[String, List[String]]]
)
object Rows {
  implicit val encoder: JsonEncoder[Rows] =
    DeriveJsonEncoder.gen[Rows]
}

case class JsonBody(data: Rows)

object JsonBody {
  implicit val encoder: JsonEncoder[JsonBody] =
    DeriveJsonEncoder.gen[JsonBody]
}
