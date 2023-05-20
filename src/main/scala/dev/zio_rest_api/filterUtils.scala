package dev.zio_rest_api
import zio.ZIOAppDefault
import zio.http.QueryParams
import zio.Chunk

object filterUtils {

  case class FilterQueryParams(filters: QueryParams)

  implicit class MapExtensions(map: Map[String, Chunk[String]]) {
    // create my own mapKeys method for queryParams
    def mapKeys[A](f: String => A): Map[A, Chunk[String]] = {
      map.map { case (key, value) => f(key) -> value }
    }
  }

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

  def checkIfRowMeetsFilterCondition(field: Field, filter: Filter, row: Row) = {
    row.entry.get(field.name) match
      case Some(value: String) if value == filter.rawString => true
      case _                                                => false

  }

  def applyAllFilters(
      filterParams: FilterQueryParams,
      rows: List[Row]
  ): List[Row] = {
    val fields = rows(0).entry.keys

    val allFilters =
      fields.flatMap(field => getFieldFilters(Field(field), filterParams))

    allFilters.foldLeft(rows) { (acc, fieldAndFilterParameter) =>
      acc.filter(row =>
        checkIfRowMeetsFilterCondition(
          fieldAndFilterParameter.field,
          fieldAndFilterParameter.filter,
          row
        )
      )
    }
  }

}
