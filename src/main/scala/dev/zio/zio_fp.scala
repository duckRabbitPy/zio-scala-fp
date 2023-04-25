package zio_fp

//> using scala "3"
//> using lib "dev.zio::zio::2.0.2"
// run cli program like this:
//     scala-cli src/main/scala/dev/zio/zio_fp.scala
//     scala-cli src/main/scala/dev/zio/zio_fp.scala --watch

// use .debug on ZIOs to unwrap values and print unwrapped values!
// must define run for Zio to run program
// using a for comp is like flatmapping each ZIO and using the success value

import actions.{dbLocation, help, viewFile, writeToFile}
import helpers.{getEmptyStream, getUserCommand}
import zio.{Console, Scope, ZIO, ZIOAppArgs, ZIOAppDefault}
import zio.connect.file._

import java.io.IOException
import java.nio.file.Paths

object zioTodo extends ZIOAppDefault {

  private val greeting: ZIO[Any, IOException, Unit] = {
    Console.printLine("Welcome to my Zio CLI tool!" + "\n\n(Commands: v, a \"task\", d \"task number\", q, help)")
  }

  def repeatLoop() = {
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
      case "v" =>
        viewFile().flatMap(_ => repeatLoop())
      case "h" =>
        help().flatMap(_ => repeatLoop())
      case "clear" =>
        for {
           emptyStream <- getEmptyStream()
           _ <- emptyStream >>> writePath(Paths.get(dbLocation + "db1.txt"))
           _ <- repeatLoop()
        }
          yield()
      case "q" =>
        ZIO.succeed(println("Goodbye!"))
      case _ => repeatLoop()
    }
  }

  val myService = for {
    _ <- greeting
    command <- getUserCommand()
    _ <- handleUserCommand(command)
  } yield ()


  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    myService.provideLayer(zio.connect.file.fileConnectorLiveLayer)
  }
}