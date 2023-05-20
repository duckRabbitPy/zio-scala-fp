package dev.zio_rest_api

import zio.ZIOAppDefault
import com.github.tototoshi.csv.CSVReader
import com.github.tototoshi.csv.CSVFormat
import com.github.tototoshi.csv.defaultCSVFormat

import zio.ZIO
import java.io.File

object fileUtils {

  def readCSVwithHeaders(
      csvFilePath: String
  ): ZIO[Any, Throwable, List[Row]] = {
    val reader = ZIO
      .attempt(CSVReader.open(new File(csvFilePath)))

    reader.map(safeReader => safeReader.allWithHeaders().map(r => Row(r)))

  }

  def sanitiseCSV(rows: List[Row]) = {
    rows.filter { row =>
      row.entry.get("id") match {
        case Some(id) if id.nonEmpty => true
        case _                       => false
      }
    }

  }

}
