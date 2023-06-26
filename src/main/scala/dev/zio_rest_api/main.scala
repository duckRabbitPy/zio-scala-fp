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
import com.github.tototoshi.csv.defaultCSVFormat
import fileUtils._
import sortUtils._
import filterUtils._
import pagination._
import zio.http.Header.Date
import java.time.format.DateTimeFormatter
import com.github.tototoshi.csv.CSVWriter

object main extends ZIOAppDefault {

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

  def respondOrServerError(
      fallableResponse: ZIO[Any, Throwable, Response]
  ) = {
    fallableResponse.fold(
      fail =>
        Response(
          Status.InternalServerError,
          body = Body.fromString(fail.getMessage())
        ),
      response => response
    )

  }

  val zioApiGetHandler: Http[Any, Response, Request, Response] =
    Http.collectZIO {

      case req @ Method.GET -> !! / "mushrooms" =>
        val maybeResponse = for {
          untypedRows <- processCSVStrings(req, "mushrooms")
          typedRows <- applyTypeSchema[MushroomSchema](untypedRows)
        } yield (
          Response
            .json(typedRows.toJson)
        )

        respondOrServerError(maybeResponse)

      case req @ Method.GET -> !! / "mushrooms" / "metadata" =>
        getMetaData(
          req,
          "mushrooms"
        )

      case req @ Method.GET -> !! / "frogs" =>
        val maybeResponse = for {
          untypedRows <- processCSVStrings(req, "frogs")
          typedRows <- applyTypeSchema[FrogSchema](untypedRows)
        } yield (
          Response
            .json(typedRows.toJson)
        )

        respondOrServerError(maybeResponse)

      case req @ Method.GET -> !! / "frogs" / "metadata" =>
        getMetaData(
          req,
          "frogs"
        )

    }

  val zioApiPostHandler: Http[Any, Response, Request, Response] =
    Http.collectZIO { case req @ Method.POST -> !! / "mushrooms" =>
      def writeToCSV(
          maybeMushrooms: Either[String, List[MushroomSchema]],
          safeWriter: CSVWriter
      ): ZIO[Any, Throwable, Unit] = {

        maybeMushrooms match {
          case Right(mushrooms) =>
            ZIO.succeed(
              mushrooms
                .map(mushroom =>
                  List(
                    mushroom.id.toString,
                    mushroom.mushroom_name,
                    mushroom.habitat,
                    mushroom.culinary_score.toString,
                    mushroom.last_updated,
                    mushroom.endangered.toString
                  )
                )
                .foreach(untypedRow => safeWriter.writeRow(untypedRow))
            )
          case Left(err) => ZIO.fail(new Exception(err))
        }
      }

      val resourcePath =
        getCSVPath("mushrooms").getOrElse("invalid path")

      val writer = ZIO
        .attempt(CSVWriter.open(new File(resourcePath), append = true))

      val maybeResponse = for {
        inputStr <- req.body.asString
        jsonInput <- ZIO.succeed(inputStr.fromJson[List[MushroomSchema]])
        safeWriter <- writer
        _ <- writeToCSV(jsonInput, safeWriter)

      } yield (
        Response
          .text(
            "hello"
          )
      )

      respondOrServerError(maybeResponse).debug("j")

    }

  val PORT = 9000

  override val run =
    for {
      _ <- printLine(s"server starting on port http://localhost:${PORT}/")
      _ <- Server
        .serve(zioApiGetHandler ++ zioApiPostHandler)
        .provide(Server.defaultWithPort(PORT))
    } yield ()
}
