import zio.{Task, ZIO}
import zio.stream.ZStream

import java.io.IOException
import java.nio.file.Path
import scala.io.StdIn
import zio.connect.file._

import scala.util.Try

package object helpers {

  def resourceToZStream(path: Path): ZStream[FileConnector, IOException, Byte] =
    readPath(path)

  def getEmptyStream(): ZIO[FileConnector, Throwable, ZStream[Any, IOException, Byte]] = {
    ZIO.succeed(ZStream.empty)
  }

  def getUserCommand(): ZIO[Any, Throwable, String] = {
    ZIO.attempt(StdIn.readLine("Enter command here: "))
  }

  def checkSafeLineNumber(str: String): Task[Int] = {
    ZIO
      .fromTry(Try(str.toInt))
      .flatMap(num =>
        if (num == 0)
          ZIO.fail(new Exception("Line number must be above 1"))
        else
          ZIO.succeed(num)
      )
  }

}
