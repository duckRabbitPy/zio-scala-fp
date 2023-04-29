package zio_fp

//> using scala "3"
//> using lib "dev.zio::zio::2.0.2"
// run cli program like this:
//     scala-cli src/main/scala/dev/zio/main.scala
//     scala-cli src/main/scala/dev/zio/main.scala --watch

// use .debug on ZIOs to unwrap values and print unwrapped values!
// must define run for Zio to run program
// using a for comp is like flatmapping each ZIO and using the success value

import actions.{greeting, handleUserCommand}
import helpers.getUserCommand
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

object myApp extends ZIOAppDefault {


  val myService = for {
    _ <- greeting
    command <- getUserCommand()
    _ <- handleUserCommand(command)
  } yield ()


  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    myService.provideLayer(zio.connect.file.fileConnectorLiveLayer)
  }
}