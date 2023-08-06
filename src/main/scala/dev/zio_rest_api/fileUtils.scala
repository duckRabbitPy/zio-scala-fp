package dev.zio_rest_api

import zio.ZIOAppDefault
import com.github.tototoshi.csv.CSVReader
import com.github.tototoshi.csv.CSVFormat
import com.github.tototoshi.csv.defaultCSVFormat

import zio.ZIO
import java.io.File
import scala.io.Source

object fileUtils {

  def getCSVPath(route: String): Option[String] = {
    val runningInDocker = sys.env.get("RUNNING_IN_DOCKER").exists(_.toBoolean)
    val baseDir =
      if (runningInDocker) "/app/src/main/resources/"
      else "src/main/resources/"

    route match {
      case "mushrooms" => Some(s"$baseDir/CSVmushroomStore.csv")
      case "frogs"     => Some(s"$baseDir/CSVfrogStore.csv")
      case _           => None
    }
  }

  def readCSVwithHeaders(
      csvFilePath: String
  ): ZIO[Any, Throwable, List[UntypedRow]] = {
    val reader = ZIO
      .attempt(CSVReader.open(new File(csvFilePath)))

    reader.map(safeReader =>
      safeReader.allWithHeaders().map(r => UntypedRow(r))
    )

  }

  def getCSVHeaders(csvFilePath: String): ZIO[Any, Throwable, List[String]] = {
    val reader = ZIO
      .attempt(CSVReader.open(new File(csvFilePath)))

    reader.map(safeReader =>
      safeReader.readNext().getOrElse(List("No headers found!"))
    )

  }

  def getTotalRowCount(csvFilePath: String): ZIO[Any, Throwable, Int] = {
    val reader = ZIO
      .attempt(CSVReader.open(new File(csvFilePath)))

    reader.map(safeReader => safeReader.allWithHeaders().length)

  }
  case class CSVResourceError(message: String) extends Exception(message)

  def checkCSVHeaders(headers: List[String]): ZIO[Any, Throwable, Unit] = {
    if (!headers.contains("id")) {
      ZIO.fail(CSVResourceError("CSV resource error. Header missing id field"))
    } else {
      ZIO.succeed(())
    }
  }

  def sanitiseCSV(rows: List[UntypedRow]): List[UntypedRow] = {
    rows.filter { row =>
      row.entry.get("id") match {
        case Some(id) if id.nonEmpty => true
        case _                       => false
      }
    }

  }

}
