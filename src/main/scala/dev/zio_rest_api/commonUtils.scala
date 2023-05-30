package dev.zio_rest_api

import scala.util.Try
import java.time.LocalDate

def canParseToInt(str: String): Boolean = {
  Try(str.toInt).isSuccess
}

def canParseToDate(str: String): Boolean = {
  Try(LocalDate.parse(str)).isSuccess
}
