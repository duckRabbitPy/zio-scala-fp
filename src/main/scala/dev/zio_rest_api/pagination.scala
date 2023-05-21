package dev.zio_rest_api

import zio.http.QueryParams
import zio.Chunk

object pagination {

  case class PaginationParams(limit: Int, offset: Int)

  def maybeChunkToInt(maybeChunk: Option[Chunk[String]], defaultInt: Int) = {

    maybeChunk
      .map(chunk => chunk.toList)
      .map(list => list.head.toInt)
      .getOrElse(defaultInt)
  }

  def getPaginationParameters(
      queryParams: QueryParams
  ): PaginationParams = {

    val limit = maybeChunkToInt(
      queryParams
        .get("limit"),
      30
    )

    val offset = maybeChunkToInt(
      queryParams
        .get("offset"),
      0
    )

    PaginationParams(limit, offset)

  }

  def applyPagination(
      pagination: PaginationParams,
      rows: List[Row]
  ) = {

    val startIndex = pagination.offset
    val minIndex = startIndex

    rows.zipWithIndex.collect {

      case (row, index)
          if index >= minIndex && (index < (minIndex + pagination.limit)) =>
        row
    }

  }

}
