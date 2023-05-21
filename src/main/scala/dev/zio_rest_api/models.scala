package dev.zio_rest_api

import zio.json.JsonEncoder
import zio.json.DeriveJsonEncoder
import zio.ZIOAppDefault
import java.time.format.DateTimeFormatter
import java.time.LocalDate

sealed trait Value
case class IntValue(value: Int) extends Value
case class BoolValue(value: Boolean) extends Value
case class StringValue(value: String) extends Value
case class LocalDateValue(value: LocalDate) extends Value

trait SchemaMapper[A] {
  def mapFields(row: UntypedRow): A
}

def applyTypeSchema[A](
    rows: List[UntypedRow]
)(implicit mapper: SchemaMapper[A]): List[A] = {
  rows.flatMap(row => List(mapper.mapFields(row)))
}

case class MushroomSchema(
    id: Int,
    mushroom_name: String,
    habitat: String,
    culinary_score: Int,
    last_updated: String,
    endangered: Boolean
)

implicit val mushroomSchemaMapper: SchemaMapper[MushroomSchema] =
  new SchemaMapper[MushroomSchema] {
    def mapFields(row: UntypedRow): MushroomSchema = {
      MushroomSchema(
        id = row.entry.getOrElse("id", "").toInt,
        mushroom_name = row.entry.getOrElse("mushroom_name", ""),
        habitat = row.entry.getOrElse("habitat", ""),
        culinary_score = row.entry.getOrElse("culinary_score", "").toInt,
        last_updated = row.entry.getOrElse("last_updated", ""),
        endangered = row.entry.get("endangered").exists(_.toBoolean)
      )
    }
  }

case class FrogSchema(
    id: Int,
    frog_name: String,
    habitat: String,
    leap_score: Int,
    last_updated: String,
    endangered: Boolean
)

implicit val frogSchemaMapper: SchemaMapper[FrogSchema] =
  new SchemaMapper[FrogSchema] {
    def mapFields(row: UntypedRow): FrogSchema = {
      FrogSchema(
        id = row.entry.getOrElse("id", "").toInt,
        frog_name = row.entry.getOrElse("frog_name", ""),
        habitat = row.entry.getOrElse("habitat", ""),
        leap_score = row.entry.getOrElse("leap_score", "").toInt,
        last_updated = row.entry.getOrElse("last_updated", ""),
        endangered = row.entry.get("endangered").exists(_.toBoolean)
      )
    }
  }

object MushroomSchema {
  implicit val encoder: JsonEncoder[MushroomSchema] =
    DeriveJsonEncoder.gen[MushroomSchema]
}

object FrogSchema {
  implicit val encoder: JsonEncoder[FrogSchema] =
    DeriveJsonEncoder.gen[FrogSchema]
}

case class UntypedRow(entry: Map[String, String])
object UntypedRow {
  implicit val encoder: JsonEncoder[UntypedRow] =
    DeriveJsonEncoder.gen[UntypedRow]
}

case class MetaData(fields: List[String], totalCount: Int)

object MetaData {
  implicit val encoder: JsonEncoder[MetaData] =
    DeriveJsonEncoder.gen[MetaData]
}

case class Field(name: String)

enum DefinedSortOption(val value: String):
  case asc extends DefinedSortOption("asc")
  case dsc extends DefinedSortOption("dsc")

case class SortOption(option: DefinedSortOption)

case class JsonBody(data: List[MushroomSchema])

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
