package dev.zio_rest_api
import zio.ZIOAppDefault
import zio.http.QueryParams
import zio.Chunk

object filterUtils {

  case class FilterQueryParams(filters: QueryParams)

  val reservedKeyWords = List("sort", "limit", "offset")
  def getFieldAndFilterParameters(
      queryParams: QueryParams
  ): FilterQueryParams = {
    FilterQueryParams(
      queryParams
        .filter(key => !reservedKeyWords.contains(key._1))
    )
  }

  def getFieldFilters(
      field: Field,
      filterQueryParams: FilterQueryParams
  ) = {
    val maybeConditions =
      filterQueryParams.filters.get(field.name).map(params => params.toList)

    maybeConditions match {
      case Some(conditions) =>
        conditions.map(condition =>
          FieldAndFilterParameter(Field(field.name), Filter(condition))
        )
      case _ => List.empty
    }

  }

  def valuesCompariable(a: String, b: String): Boolean = {
    canParseToInt(a) && canParseToInt(b)
  }

  def meetsCondition(value: String, filterCondition: Filter): Boolean = {

    val comparisonLogic = filterCondition.comparisonLogic

    comparisonLogic.exists { comparison =>
      comparison.operator match {
        case Operator.isEqualTo => value == comparison.value
        case Operator.greaterThan
            if valuesCompariable(value, comparison.value) =>
          value > comparison.value
        case Operator.greaterThanOrEqualTo
            if valuesCompariable(value, comparison.value) =>
          value >= comparison.value
        case Operator.lessThan if valuesCompariable(value, comparison.value) =>
          value < comparison.value
        case Operator.lessThanOrEqualTo
            if valuesCompariable(value, comparison.value) =>
          value <= comparison.value
        case _ => true

      }
    }
  }

  def checkIfRowMeetsFilterCondition(
      allFilters: Iterable[FieldAndFilterParameter],
      row: UntypedRow
  ) = {
    allFilters match
      case allFilters
          if allFilters
            .map(fromQueryParam => {
              row.entry.get(fromQueryParam.field.name) match
                case Some(value: String)
                    if meetsCondition(value, fromQueryParam.filter) =>
                  true
                case _ => false
            })
            .forall(_ == true) =>
        true
      case _ => false

  }

  def applyAllFilters(
      filterParams: FilterQueryParams,
      rows: List[UntypedRow]
  ): List[UntypedRow] = {
    val fields = rows(0).entry.keys

    val allFilters =
      fields.flatMap(field => getFieldFilters(Field(field), filterParams))

    rows.filter(row => checkIfRowMeetsFilterCondition(allFilters, row))

  }

}
