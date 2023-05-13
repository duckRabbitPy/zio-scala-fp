package dev.zio_http_app

import actions.dbLocation
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
import zio.ZIOApp

object zio_http_app extends ZIOAppDefault {

  val Store =
    Paths.get(System.getProperty("user.dir") + "/src/main/resources/db1.txt")

  val History = Paths.get(
    System.getProperty("user.dir") + "/src/main/resources/versionHistory.txt"
  )

  val taskSeperator = "-----------------------"

  def getFileContentString(
      filePath: Path
  ): ZIO[FileConnector, Throwable, String] = {
    readPath(filePath).runCollect.flatMap { bytes =>
      ZIO.attempt(new String(bytes.toArray, StandardCharsets.UTF_8))
    }

  }

  def contentStringToZStream(string: String) = {
    ZStream
      .fromInputStream(
        new ByteArrayInputStream(
          string
            .getBytes(StandardCharsets.UTF_8)
        )
      )
  }

  def ZBytestreamToContentString(
      stream: ZStream[Any, Any, Byte]
  ): ZIO[Any, Any, String] = {
    stream.runCollect.map(bytes => {
      new String(bytes.toArray, StandardCharsets.UTF_8)
    })

  }

  def addSeperator(
      str: String,
      seperator: String
  ) = {
    str match {
      case "" => Left(None)
      case _  => Right(str + seperator)
    }

  }

  def appendToExistingFile(
      content: String,
      targetFilePath: Path,
      seperator: String = "\n"
  ): ZIO[FileConnector, Throwable, ZStream[Any, Throwable, Byte]] = {

    val x = addSeperator(content, seperator).getOrElse("EMPTY ENTRY!" + "\n")
    val throwableNewStream =
      getFileContentString(targetFilePath)
        .map(fileContent =>
          contentStringToZStream(
            fileContent.concat(
              x
            )
          )
        )

    for {
      maybeNewStream <- throwableNewStream
      _ <- maybeNewStream >>> writePath(targetFilePath)
    } yield (maybeNewStream)

  }

  def checkIsSafeLineNumber(maybeLineNumber: String) = {
    ZIO.attempt(maybeLineNumber.toInt)
  }

  def saveToVersionHistory(stream: ZStream[Any, Throwable, Byte]) = {
    for {
      txt <- ZBytestreamToContentString(stream)
      _ <- appendToExistingFile(
        txt,
        History,
        "\n" + "Updated at: " + LocalDateTime.now().toString() + "\n"
      )
      _ <- appendToExistingFile(
        "\n" +
          taskSeperator,
        History
      )
    } yield stream

  }

  def writeToTxtStore(
      stream: ZStream[Any, IOException, Byte]
  ) = {
    for {
      _ <- stream >>> writePath(Paths.get(dbLocation + "db1.txt"))
      _ <- saveToVersionHistory(stream)
    } yield stream

  }

  def deleteLineAndReturnFile(
      lineNumber: String
  ): ZIO[FileConnector, Any, ZStream[Any, IOException, Byte]] = {

    val throwableNewStream = for {
      contentString <- getFileContentString(Store)
      safeLineNumber <- checkIsSafeLineNumber(lineNumber)
    } yield contentStringToZStream(
      contentString
        .split("\n")
        .toList
        .zipWithIndex
        .filter(itemIndexTuple => itemIndexTuple._2 != safeLineNumber)
        .map(_._1)
        .mkString("\n")
    )

    for {
      stream <- throwableNewStream
      _ <- writeToTxtStore(stream)
    } yield stream

  }

  def foldIntoResponse(maybeResult: ZIO[Any, Any, Response]) = {
    maybeResult.fold(
      _ => Response.status(Status.BadRequest),
      successResponse => successResponse
    )
  }

  val app: App[Any] =
    Http.collect[Request] {
      case Method.GET -> !! / "text" => Response.text("Hello World!")
      case Method.GET -> !! =>
        val htmlFile = Paths.get("src/main/resources/client.html")
        val htmlFileStream: ZStream[Any, Throwable, Byte] =
          ZStream.fromInputStream(Files.newInputStream(htmlFile))
        Response(
          status = Status.Ok,
          headers = Headers.empty,
          body = Body.fromStream(htmlFileStream)
        )

      case Method.GET -> !! / "json-payload" =>
        Response.json("""{"payload": "here are some tasks"}""")
      case Method.GET -> !! / "alltasks" =>
        Response(
          status = Status.Ok,
          body = Body.fromFile(
            new File(
              "/Users/duck_rabbit/Desktop/Scala/learning-scala-fp/src/main/resources/db1.txt"
            )
          ),
          headers = Headers.empty
        )
      case Method.GET -> !! / "params" =>
        Response(
          Status.Ok,
          body =
            Body.fromString(QueryParams("q" -> Chunk("a", "b", "c")).toString),
          headers = Headers.empty
        )

    }

  def penultimateItemInArray(array: Array[String]): String = {
    if (array.length >= 2) array(array.length - 3)
    else array(1)

  }

  val collectZIOApp: Http[Any, Response, Request, Response] = Http.collectZIO {
    case req @ Method.GET -> !! / "undo" => {
      val maybeLastList = for {
        pastLists <- getFileContentString(History)
      } yield (Response(
        body = Body
          .fromString(penultimateItemInArray(pastLists.split(taskSeperator))),
        status = Status.Ok
      ))

      foldIntoResponse(
        maybeLastList.provideLayer(
          zio.connect.file.fileConnectorLiveLayer
        )
      )

    }
    case req @ Method.POST -> !! / "add-task" => {
      val maybeAddedTaskList = for {
        input <- req.body.asString
        newFileContent <- appendToExistingFile(input, Store)
        _ <- saveToVersionHistory(newFileContent)
        newTasks <- ZBytestreamToContentString(newFileContent)
      } yield (
        Response(body = Body.fromString(newTasks), status = Status.Ok)
      )

      foldIntoResponse(
        maybeAddedTaskList.provideLayer(
          zio.connect.file.fileConnectorLiveLayer
        )
      )

    }

    case req @ Method.POST -> !! / "delete-task" => {
      val maybeNewFileContent = for {
        lineNumber <- req.body.asString
        updatedFileContent <- deleteLineAndReturnFile(lineNumber).provideLayer(
          zio.connect.file.fileConnectorLiveLayer
        )
      } yield (updatedFileContent)

      val maybeNewList = for {
        newFileContent <- maybeNewFileContent
        newTasks <- ZBytestreamToContentString(newFileContent)
      } yield (
        Response(body = Body.fromString(newTasks), status = Status.Ok)
      )

      foldIntoResponse(maybeNewList)
    }

  }

  val PORT = 9000
  override val run =
    for {
      _ <- printLine(s"server starting on port http://localhost:${PORT}/")
      _ <- Server
        .serve(app ++ collectZIOApp)
        .provide(Server.defaultWithPort(PORT))
    } yield ()

}
