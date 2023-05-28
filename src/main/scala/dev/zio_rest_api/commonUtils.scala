package dev.zio_rest_api

import scala.util.Try
import java.time.LocalDate

def canParseToInt(str: String): Boolean = {
  Try(str.toInt).isSuccess
}

def canParseToDate(str: String): Boolean = {
  Try(LocalDate.parse(str)).isSuccess
}

def getCaseClassFields[T](caseClass: T): Map[String, String] = {
  val fields = caseClass.getClass.getDeclaredFields
  fields.map { field =>
    field.setAccessible(true)
    val fieldName = field.getName
    val fieldType = field.getType.getSimpleName
    fieldName -> fieldType
  }.toMap
}
