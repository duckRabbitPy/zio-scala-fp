package dev.zio_rest_api

import zio.ZIOAppDefault
import com.github.tototoshi.csv.CSVReader
import com.github.tototoshi.csv.CSVFormat
import com.github.tototoshi.csv.defaultCSVFormat

import zio.ZIO
import java.io.File

object fileUtils {

  val Store =
    System.getProperty(
      "user.dir"
    ) + "/src/main/resources/CSVstore.csv"

  def readCSVwithHeaders(): ZIO[Any, Throwable, List[Row]] = {
    val reader = ZIO
      .attempt(CSVReader.open(new File(Store)))

    reader.map(safeReader => safeReader.allWithHeaders().map(r => Row(r)))

  }

}
