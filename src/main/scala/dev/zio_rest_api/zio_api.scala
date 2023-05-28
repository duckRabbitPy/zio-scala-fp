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
  val MushroomSchemaInstance = MushroomSchema(
    id = 100000,
    mushroom_name = "exampleMushroom",
    culinary_score = 7,
    habitat = "example",
    last_updated = "example",
    endangered = false
  )

  def processCSVStrings(
      req: Request,
      resource: String
  ): ZIO[Any, Throwable, List[UntypedRow]] = {
    val filterParams = getFieldAndFilterParameters(req.url.queryParams)
    val sortParams = getFieldAndSortParameters(req.url.queryParams)
    val paginationParams = getPaginationParameters(req.url.queryParams)
    val resourcePath =
      getCSVPath(resource).getOrElse("invalid path")

    for {
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

    } yield paginatedRows

  }

  def getMetaData(
      req: Request,
      resource: String
  ) = {
    val resourcePath =
      getCSVPath(resource).getOrElse("invalid path")

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

  val zioApi: Http[Any, Response, Request, Response] = Http.collectZIO {

    case req @ Method.GET -> !! / "mushrooms" =>
      val maybeResponse = for {
        untypedRows <- processCSVStrings(req, "mushrooms")
      } yield (
        Response
          .json(applyTypeSchema[MushroomSchema](untypedRows).toJson)
      )

      maybeResponse.fold(
        fail =>
          Response(
            Status.InternalServerError,
            body = Body.fromString(fail.getMessage())
          ),
        response => response
      )

    case req @ Method.GET -> !! / "mushrooms" / "metadata" =>
      getMetaData(
        req,
        "mushrooms"
      )

    case req @ Method.GET -> !! / "frogs" =>
      val maybeResponse = for {
        untypedRows <- processCSVStrings(req, "frogs")
      } yield (
        Response
          .json(applyTypeSchema[FrogSchema](untypedRows).toJson)
      )

      val x = maybeResponse.fold(
        fail =>
          Response(
            Status.InternalServerError,
            body = Body.fromString(fail.getMessage())
          ),
        response => response
      )

    case req @ Method.GET -> !! / "frogs" / "metadata" =>
      getMetaData(
        req,
        "frogs"
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
