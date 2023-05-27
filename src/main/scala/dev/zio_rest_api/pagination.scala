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
      rows: List[UntypedRow]
  ) = {

    pagination.offset match {
      case offset if offset < rows.length =>
        rows.drop(offset).take(pagination.limit)
      case _ =>
        List.empty[UntypedRow]
    }

  }

}
