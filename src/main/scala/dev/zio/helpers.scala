import zio.ZIO
import zio.stream.ZStream
import java.io.IOException
import java.nio.file.Path
import scala.io.StdIn
import zio.connect.file._

package object helpers {

  def resourceToZStream(path: Path): ZStream[FileConnector, IOException, Byte] =
    readPath(path)

  def getEmptyStream(): ZIO[FileConnector, Throwable, ZStream[Any, IOException, Byte]] = {
    ZIO.succeed(ZStream.empty)
  }

  def getUserCommand(): ZIO[Any, Throwable, String] = {
    ZIO.attempt(StdIn.readLine("Enter command here: "))
  }



}