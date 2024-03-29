package dev.zio_rest_api
import zio.ZIOAppDefault
import zio.http.QueryParams
import zio.Chunk
import scala.util.Try
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object sortUtils {

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

  def getFieldAndSortParameters(
      queryParams: QueryParams
  ): List[FieldAndSortParameter] = {
    val maybeFieldAndSortParameters = queryParams
      .get("sort")
      .map(delimitedStrings =>
        (delimitedStrings
          .map(delimitedString => parseColonDelimitedString(delimitedString)))
      )

    processFieldAndSortParameters(
      maybeFieldAndSortParameters
    )

  }

  def sortByField(
      field: Field,
      sortDirection: DefinedSortOption,
      data: List[UntypedRow]
  ): List[UntypedRow] = {
    val dateFormat = DateTimeFormatter.ISO_DATE

    val sortedData = data.sortBy { row =>
      row.entry.get(field.name) match {
        case Some(numericStr: String) if canParseToInt(numericStr) =>
          numericStr.toInt
        case Some(dateStr: String) if canParseToDate(dateStr) =>
          LocalDate.parse(dateStr).toEpochDay
        case Some(alphaNumericStr: String) => alphaNumericStr
        case _ =>
          Int.MaxValue
      }

    }(Ordering.fromLessThan { (a, b) =>
      (a, b) match {
        case (numericA: Int, numericB: Int) => numericA < numericB
        case (dateA: Long, dateB: Long)     => dateA < dateB
        case (alphaNumericA: String, alphaNumericB: String) =>
          alphaNumericA < alphaNumericB
        case _ => false
      }
    })

    sortDirection match {
      case DefinedSortOption.asc => sortedData
      case DefinedSortOption.dsc => sortedData.reverse
    }
  }

  def applyAllSortParams(
      fieldAndSortParameters: List[FieldAndSortParameter],
      data: List[UntypedRow]
  ): List[UntypedRow] = {
    fieldAndSortParameters.foldLeft(data) { (acc, fieldAndSortParameters) =>
      sortByField(
        fieldAndSortParameters.field,
        fieldAndSortParameters.order,
        acc
      )

    }
  }

}
