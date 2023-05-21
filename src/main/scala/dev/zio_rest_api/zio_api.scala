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
import zio.http.Header.Date
import java.time.format.DateTimeFormatter

object zio_http_app extends ZIOAppDefault {
  val testRouteForDev = "mushrooms"
  val zioApi: Http[Any, Response, Request, Response] = Http.collectZIO {

    case req @ Method.GET -> !! / testRouteForDev =>
      val filterParams = getFieldAndFilterParameters(req.url.queryParams)
      val sortParams = getFieldAndSortParameters(req.url.queryParams)
      val paginationParams = getPaginationParameters(req.url.queryParams)
      val resourcePath =
        getCSVPath(testRouteForDev).getOrElse("invalid path")

      val proccessedData = for {
        headers <- getCSVHeaders(
          resourcePath
        )
        _ <- checkCSVHeaders(headers)
        rows <- readCSVwithHeaders(
          resourcePath
        )
        sanitizedRows = sanitiseCSV(rows)
        filteredRows = applyAllFilters(filterParams, sanitizedRows)
        sortedRows = applyAllSortParams(sortParams, filteredRows)
        paginatedRows = applyPagination(paginationParams, sortedRows)
        typedRows = applyTypeSchema[MushroomSchema](
          paginatedRows
        )

      } yield typedRows

      proccessedData
        .fold(
          fail =>
            Response(
              Status.InternalServerError,
              body = Body.fromString(fail.getMessage())
            ),
          data => Response.json(data.toJson)
        )
        .debug("error")

    case req @ Method.GET -> !! / testRouteForDev / "metadata" =>
      val resourcePath =
        getCSVPath(testRouteForDev).getOrElse("invalid path")

      val proccessedData = for {
        headers <- getCSVHeaders(
          resourcePath
        )
        totalCount <- getTotalRowCount(resourcePath)

      } yield MetaData(headers, totalCount)

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
