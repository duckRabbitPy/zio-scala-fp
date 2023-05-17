package dev.zio_rest_api

import zio.ZIOAppDefault
import com.github.tototoshi.csv.CSVReader
import zio.ZIO
import java.io.File

object fileUtils {

  def parseIntoRows(
      allWithHeaders: List[Map[String, String]]
  ): Rows = {
    val grouped = allWithHeaders.map { kvp =>
      kvp.groupBy(_._1).mapValues(_.values.toList)
    }
    Rows(grouped)

  }

  val Store =
    System.getProperty(
      "user.dir"
    ) + "/src/main/resources/CSVstore.csv"

  def readCSVwithHeaders() = {
    val reader = ZIO
      .attempt(CSVReader.open(new File(Store)))

    reader.map(safeReader => safeReader.allWithHeaders()).debug("withHeaders")

  }

}
