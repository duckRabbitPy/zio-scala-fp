import helpers.resourceToZStream
import zio.stream.ZStream
import zio.{Console, ZIO}
import zio.connect.file._
import java.io.{ByteArrayInputStream, IOException}
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

package object actions  {

  val dbLocation = System.getProperty("user.dir") + "/src/main/resources/"


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


  def writeToFile(content: String): ZIO[FileConnector, Throwable, ZStream[Any, IOException, Byte]] = {
    val oldFileStream = resourceToZStream(Paths.get(dbLocation + "db1.txt")).runCollect.flatMap { bytes =>
      ZIO.attempt(new String(bytes.toArray, StandardCharsets.UTF_8))
    }

    for {
      contentString <- oldFileStream
    }
    yield (ZStream.fromInputStream(new ByteArrayInputStream(contentString.concat("\n" ++ content).getBytes(StandardCharsets.UTF_8))))

  }

}
