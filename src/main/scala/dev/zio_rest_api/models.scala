package dev.zio_rest_api

import zio.json.JsonEncoder
import zio.json.DeriveJsonEncoder
import zio.ZIOAppDefault

case class Mushroom(name: String)
object Mushroom {
  implicit val encoder: JsonEncoder[Mushroom] =
    DeriveJsonEncoder.gen[Mushroom]
}

case class Row(entry: Map[String, String])

object Row {
  implicit val encoder: JsonEncoder[Row] =
    DeriveJsonEncoder.gen[Row]
}

case class Field(name: String)

enum DefinedSortOption(val value: String):
  case asc extends DefinedSortOption("asc")
  case dsc extends DefinedSortOption("dsc")

case class SortOption(option: DefinedSortOption)

case class JsonBody(data: List[Row])

object JsonBody {
  implicit val encoder: JsonEncoder[JsonBody] =
    DeriveJsonEncoder.gen[JsonBody]
}

case class FieldAndFilterParameter(field: Field, filter: Filter)

case class Filter(rawString: String)
