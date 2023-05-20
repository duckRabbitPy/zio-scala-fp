package dev.zio_rest_api

import zio.http.QueryParams
import zio.Chunk

object pureUtils {

  def getSortOption(str: String): DefinedSortOption =
    str match
      case DefinedSortOption.asc.value | DefinedSortOption.dsc.value =>
        DefinedSortOption.valueOf(str)
      case _ => DefinedSortOption.asc

  def parseColonDelimitedString(
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

  def processFieldAndSortParameters(
      maybefieldAndSortParameters: Option[
        Chunk[Option[(Field, DefinedSortOption)]]
      ]
  ): List[FieldAndSortParameter] = {
    val DefaultSorting =
      FieldAndSortParameter(Field("id"), DefinedSortOption.asc)

    maybefieldAndSortParameters match {
      case Some(chunk) =>
        val parameters = chunk.collect { case Some(parameterPair) =>
          parameterPair
        }.toList

        if (parameters.nonEmpty)
          parameters.map { case (field, sortOption) =>
            FieldAndSortParameter(field, sortOption)
          }
        else
          List(DefaultSorting)

      case None => List(DefaultSorting)
    }
  }

  def getFirstFieldAndSortParameter(
      queryParams: QueryParams
  ): FieldAndSortParameter = {
    val maybeFieldAndSortParameters = queryParams
      .get("sort")
      .map(delimitedStrings =>
        (delimitedStrings
          .map(delimitedString => parseColonDelimitedString(delimitedString)))
      )

    processFieldAndSortParameters(
      maybeFieldAndSortParameters
    )(0)
    // todo, work with mulitple sort queries

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
