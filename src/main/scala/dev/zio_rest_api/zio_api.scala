package dev.zio_rest_api

import zio.Console._
import zio.connect.file.{FileConnector, readPath, writePath}
import zio.http._
import zio.stream.ZStream
import zio.{Chunk, ZIO, ZIOAppDefault}
import java.io.{ByteArrayInputStream, File}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import _root_.java.io.IOException
import java.time.LocalDateTime
import scala.collection.immutable
import zio.json._
import com.github.tototoshi.csv.CSVReader
import fileUtils._
import sortUtils._
import filterUtils._
import pagination._

object zio_http_app extends ZIOAppDefault {

  val zioApi: Http[Any, Response, Request, Response] = Http.collectZIO {

    case req @ Method.GET -> !! / "mushrooms" =>
      val filterParams = getFieldAndFilterParameters(req.url.queryParams)
      val sortParams = getFieldAndSortParameters(req.url.queryParams)
      val paginationParams = getPaginationParameters(req.url.queryParams)

      val proccessedData = for {
        rows <- readCSVwithHeaders(
          getCSVPath("mushrooms").getOrElse("invalid path")
        )
        sanitizedRows = sanitiseCSV(rows)
        filteredRows = applyAllFilters(filterParams, sanitizedRows)
        sortedRows = applyAllSortParams(sortParams, filteredRows)
        paginatedRows = applyPagination(paginationParams, sortedRows)
      } yield paginatedRows

      proccessedData.fold(
        fail =>
          Response(
            Status.InternalServerError,
            body = Body.fromString(fail.getMessage())
          ),
        data => Response.json(data.toJson)
      )

  }

  val PORT = 9000

  override val run =
    for {
      _ <- printLine(s"server starting on port http://localhost:${PORT}/")
      _ <- Server
        .serve(zioApi)
        .provide(Server.defaultWithPort(PORT))
    } yield ()
}
