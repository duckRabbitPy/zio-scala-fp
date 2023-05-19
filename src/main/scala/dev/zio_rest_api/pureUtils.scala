package dev.zio_rest_api

object pureUtils {

  enum DefinedSortOption(val value: String):
    case Asc extends DefinedSortOption("Asc")
    case Dsc extends DefinedSortOption("Dsc")

  case class SortOption(option: DefinedSortOption)

  def getSortOption(str: String): DefinedSortOption =
    str match
      case DefinedSortOption.Asc.value | DefinedSortOption.Dsc.value =>
        DefinedSortOption.valueOf(str)
      case _ => DefinedSortOption.Asc

  def getFieldAndSortParameter(
      delimitedString: String
  ) = {
    val optionArray = delimitedString.split(":")
    optionArray match {
      case array if array.length >= 2 =>
        Some(
          (
            zio_http_app.Field(array(0)),
            getSortOption(array(1))
          )
        )
      case _ => None
    }
  }
}
