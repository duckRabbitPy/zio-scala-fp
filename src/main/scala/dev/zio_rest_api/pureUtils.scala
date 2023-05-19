package dev.zio_rest_api

object pureUtils {

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
            Field(array(0)),
            getSortOption(array(1))
          )
        )
      case _ => None
    }
  }

  def sortByField(
      fieldName: Field,
      sortDirection: DefinedSortOption,
      data: List[Row]
  ): List[Row] = {
    data.sortBy { row =>
      row.entry.get("id") match {
        case Some(id: String) => id.toInt
        case _ =>
          Int.MaxValue
      }
    }
  }
}
