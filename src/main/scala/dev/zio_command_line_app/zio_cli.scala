// package dev.zio_command_line_app

// //> using scala "3"
// //> using lib "dev.zio::zio::2.0.2"

// import actions.{greeting, handleUserCommand}
// import helpers.getUserCommand
// import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

// object zio_cli extends ZIOAppDefault {

//   val myService = for {
//     _ <- greeting
//     command <- getUserCommand()
//     _ <- handleUserCommand(command)
//   } yield ()

//   override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
//     myService.provideLayer(zio.connect.file.fileConnectorLiveLayer)
//   }
// }
