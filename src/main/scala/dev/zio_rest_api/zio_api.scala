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
import pureUtils._

object zio_http_app extends ZIOAppDefault {

  val zioApi: Http[Any, Response, Request, Response] = Http.collectZIO {
    case req @ Method.GET -> !! / "mushroom" => {
      ZIO.succeed(
        Response.json(Mushroom("amanita muscaria").toJsonPretty)
      )
    }

    case req @ Method.GET -> !! / "params" =>
      ZIO.succeed(
        Response(
          Status.Ok,
          body = Body.fromString(req.url.queryParams.toString()),
          headers = Headers.empty
        )
      )

    case req @ Method.GET -> !! / "rows" => {

      val sortParams = getFieldAndSortParameter(req.url.queryParams)

      // ZIO.succeed(Response.text(s"${sortParams.field}  ${sortParams.order}"))

      readCSVwithHeaders()
        .fold(
          fail =>
            Response
              .status(Status.InternalServerError),
          dataFromCSV => {
            Response.json(
              sortByField(
                sortParams.field,
                sortParams.order,
                dataFromCSV
              ).toJson
            )
          }
        )

    }

    case req @ Method.POST -> !! / "store" => {

      val maybeLog = for {
        logJSON <- ZIO
          .succeed(req.body.asString)
          .map(s => s.map(str => Console.println(str)))
        _ <- logJSON

      } yield (Response(body = Body.fromString("ok"), status = Status.Ok))

      maybeLog.fold(
        err =>
          Response(
            body = Body.fromString("error"),
            status = Status.InternalServerError
          ),
        ok => Response(body = Body.fromString("ok"), status = Status.Ok)
      )

    }

    case _ =>
      ZIO.succeed(
        Response(
          body = Body.fromString(
            "No route was found matching the URL and request method"
          ),
          status = Status.NotFound
        )
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
