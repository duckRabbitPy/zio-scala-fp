import helpers.{checkSafeLineNumber, getEmptyStream, getUserCommand, resourceToZStream}
import zio.connect.file._
import zio.stream.ZStream
import zio.{Console, ZIO}

import java.io.{ByteArrayInputStream, IOException}
import java.nio.charset.StandardCharsets
import java.nio.file.Paths


package object actions  {

  val dbLocation = System.getProperty("user.dir") + "/src/main/resources/"

  def greeting: ZIO[Any, IOException, Unit] = {
    Console.printLine("Welcome to my Zio CLI tool!" + "\n\n(Commands: v, a \"task\", d \"task number\", q, help)")
  }


  def help(): ZIO[Any, IOException, Unit] = {
    Console.printLine("\n" +
      """
        | Possible commands
        | -----------------
        |v - view the list of tasks
        |a <task> add a to-do item
        |d [task number] - delete a task by its number
        |clear - delete all items
        |q - quit command line tool
        |help - show this help text
        |""".stripMargin.trim + "\n")
  }

  def showInputErrorMsg(): ZIO[Any, IOException, Unit] = {
    Console.printLine("Command not recognised. Type 'help' for command info")
  }

  def viewFile(): ZIO[FileConnector, Throwable, Unit] = {
    resourceToZStream(Paths.get(dbLocation + "db1.txt")).runCollect.flatMap { bytes =>
      ZIO.attempt {
        val result = new String(bytes.toArray, StandardCharsets.UTF_8)
          .split("\n")
          .zipWithIndex
          .map { case (line, idx) =>
            if (line.nonEmpty) s"$idx: $line" else "My tasks:"
          }
          .mkString("\n")
        println(result)
      }
    }
  }

  def getCurrentFileStream(): ZIO[FileConnector, Throwable, String] = {
    resourceToZStream(Paths.get(dbLocation + "db1.txt")).runCollect.flatMap { bytes =>
      ZIO.attempt(new String(bytes.toArray, StandardCharsets.UTF_8))
    }
  }


  def writeToFile(content: String): ZIO[FileConnector, Throwable, ZStream[Any, IOException, Byte]] = {
    val currentFileStream = getCurrentFileStream()
    for {
      contentString <- currentFileStream
    }
    yield (ZStream.fromInputStream(new ByteArrayInputStream(contentString.concat("\n" ++ content).getBytes(StandardCharsets.UTF_8))))

  }

  def deleteLine(lineNumber: String) = {

    val throwableStream = for {
      contentString <- getCurrentFileStream()
      safeLineNumber <- checkSafeLineNumber(lineNumber, contentString)
    }
    yield ZStream.fromInputStream(new ByteArrayInputStream(
      contentString
          .split("\n")
          .toList.zipWithIndex
          .filter(itemIndexTuple => itemIndexTuple._2 != safeLineNumber)
          .map(_._1)
          .mkString("\n")
          .getBytes(StandardCharsets.UTF_8)))
    
    for {
      writeEffectOrIOException <- throwableStream.fold(
        err => Console.printLine(err),
        stream => stream >>> writePath(Paths.get(dbLocation + "db1.txt")))
        _ <- writeEffectOrIOException
    }
      yield ()

  }

  def repeatLoop(): ZIO[FileConnector, Throwable, Unit] = {
    getUserCommand().flatMap(nextCommand => handleUserCommand(nextCommand))
  }

  def handleUserCommand(command: String): ZIO[FileConnector, Throwable, Unit] = {
    command match {
      case add if add.startsWith("a ") =>
        for {
          newStream <- writeToFile(command.drop(2))
          _ <- newStream >>> writePath(Paths.get(dbLocation + "db1.txt"))
          _ <- viewFile()
          _ <- repeatLoop()
        }
        yield ()
      case delete if delete.startsWith("d ") =>
        for {
          _ <- deleteLine(command.drop(2))
          _ <- viewFile()
          _ <- repeatLoop()
        } yield ()
      case "v" =>
        viewFile().flatMap(_ => repeatLoop())
      case "help" =>
        help().flatMap(_ => repeatLoop())
      case "clear" =>
        for {
          emptyStream <- getEmptyStream()
          _ <- emptyStream >>> writePath(Paths.get(dbLocation + "db1.txt"))
          _ <- repeatLoop()
        }
        yield ()
      case "q" =>
        ZIO.succeed(println("Goodbye!"))
      case _ =>
        showInputErrorMsg().flatMap(_ => repeatLoop())
    }
  }


}

