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

object zio_http_app extends ZIOAppDefault {

  val zioApi: Http[Any, Response, Request, Response] = Http.collectZIO {
    case req @ Method.GET -> !! / "mushroom" => {
      ZIO.succeed(
        Response.json(Mushroom("amanita muscaria").toJsonPretty)
      )
    }

    case req @ Method.GET -> !! / "rows" => {
      readCSVwithHeaders()
        .fold(
          fail =>
            Response
              .status(Status.InternalServerError),
          dataFromCSV => {

            Response
              .json(
                JsonBody(
                  parseIntoRows(dataFromCSV)
                ).toJsonPretty
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
