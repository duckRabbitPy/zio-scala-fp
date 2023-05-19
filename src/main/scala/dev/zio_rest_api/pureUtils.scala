package dev.zio_rest_api

import zio.http.QueryParams

object pureUtils {

  def getSortOption(str: String): DefinedSortOption =
    str match
      case DefinedSortOption.asc.value | DefinedSortOption.dsc.value =>
        DefinedSortOption.valueOf(str)
      case _ => DefinedSortOption.asc

  def parseColonDelimitedParam(
      colonDelimitedString: String
  ) = {
    val optionArray = colonDelimitedString.split(":")
    optionArray match {
      case array if array.length >= 2 =>
        Some(
          (
            Field(array(0)),
            getSortOption(array(1))
          )
        )
      case _ => None
    }
  }

  case class FieldAndSortParameter(field: Field, order: DefinedSortOption)

  def getFieldAndSortParameter(
      queryParams: QueryParams
  ): FieldAndSortParameter = {
    val maybefieldAndSortParameter = queryParams
      .get("sort")
      .map(colonDelimitedChunk =>
        parseColonDelimitedParam(colonDelimitedChunk.asString)
      )
      .flatten

    val field =
      maybefieldAndSortParameter.map(pair => pair._1).getOrElse(Field("id"))

    val order = maybefieldAndSortParameter
      .map(pair => pair._2)
      .getOrElse(DefinedSortOption.asc)

    FieldAndSortParameter(field, order)

  }

  def sortByField(
      field: Field,
      sortDirection: DefinedSortOption,
      data: List[Row]
  ): List[Row] = {
    val sortedData = data.sortBy { row =>
      row.entry.get(field.name) match {
        case Some(id: String) => id.toInt
        case _ =>
          Int.MaxValue
      }
    }
    sortDirection match {
      case DefinedSortOption.asc => sortedData
      case DefinedSortOption.dsc => sortedData.reverse
    }
  }
}
