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

case class ComparisonLogic(operator: Operator, value: String)

enum Operator(val value: String):
  case isEqualTo extends Operator("eq")
  case greaterThan extends Operator("gt")
  case greaterThanOrEqualTo extends Operator("gte")
  case lessThan extends Operator("lt")
  case lessThanOrEqualTo extends Operator("lte")

def checkOperator(maybeValidOperator: String): Option[Operator] =
  Operator.values.find(_.value == maybeValidOperator)

case class Filter(rawString: String) {

  private val splitOnColon: Array[String] = rawString.split(":")

  private val validOperator: Option[Operator] =
    splitOnColon.headOption.flatMap(checkOperator)

  val comparisonLogic: Option[ComparisonLogic] = splitOnColon match {
    case Array(value) =>
      Some(ComparisonLogic(Operator.isEqualTo, value))
    case Array(operatorStr, value) if validOperator.isDefined =>
      Some(ComparisonLogic(validOperator.get, value))
    case _ =>
      None
  }
}
