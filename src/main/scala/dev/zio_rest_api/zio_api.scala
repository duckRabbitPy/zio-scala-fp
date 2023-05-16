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

object zio_http_app extends ZIOAppDefault {

  case class Mushroom(name: String)

  case class MushroomCollection(mushrooms: List[Mushroom])

  object Mushroom {
    implicit val encoder: JsonEncoder[Mushroom] =
      DeriveJsonEncoder.gen[Mushroom]
  }

  object MushroomCollection {
    implicit val encoder: JsonEncoder[MushroomCollection] =
      DeriveJsonEncoder.gen[MushroomCollection]
  }

  val Store =
    System.getProperty(
      "user.dir"
    ) + "/src/main/resources/CSVstore.csv"

  def readCSVRow(): ZIO[Any, Throwable, List[Mushroom]] = {
    val reader = ZIO
      .attempt(CSVReader.open(new File(Store)))

    val maybeCSV = reader.map(safeReader => safeReader.all())

    maybeCSV.map(rows => rows.head.map(str => Mushroom(str)))

  }

  val zioApi: Http[Any, Response, Request, Response] = Http.collectZIO {
    case req @ Method.GET -> !! / "mushroom" => {
      ZIO.succeed(
        Response.json(Mushroom("amanita muscaria").toJsonPretty)
      )
    }
    case req @ Method.GET -> !! / "mushrooms" => {
      readCSVRow()
        .fold(
          fail =>
            Response
              .status(Status.InternalServerError),
          mushroomsFromCSV =>
            Response
              .json(MushroomCollection(mushroomsFromCSV).toJsonPretty)
        )
    }
    case req @ Method.POST -> !! / "csv" => {
      val reader = ZIO
        .attempt(CSVReader.open(new File(Store)))
        .debug("store")

      val maybeCSV = reader
        .map(safeReader => Console.println(safeReader.all()))

      val good = for {
        _ <- maybeCSV
      } yield (ZIO.succeed(Response.text("mnice")))

      good.fold(
        f => Response.status(Status.BadRequest),
        s => Response.text("success")
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
