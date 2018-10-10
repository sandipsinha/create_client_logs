
import java.lang.reflect.Field

import org.apache.commons.lang3.StringEscapeUtils

/**
  * Implicit class that provides a toMap method. It will turn a case class into a map of key, values.
  * This is intended for case classes only.
  */
object Implicits {
  implicit class CaseClassToMap(c: AnyRef) {

    // --- Methods ---
    def toMap: Map[String, Any] = {
      toMap(getDefaultValue)
    }

    def toMap(formatFunction: (Field, AnyRef) => Any): Map[String, Any] = {
      (Map[String, Any]() /: c.getClass.getDeclaredFields) { (map: Map[String, Any], field: Field) =>
        field.setAccessible(true)
        val fieldValue: Any = formatFunction(field, c)
        map + (field.getName -> fieldValue)
      }
    }

    def getDefaultValue(field: Field, c: AnyRef): Any = {
      if (field.get(c) == null) {

        // Set the default values to something other than null
        if (field.getType.getName == "java.lang.String") ""
        else if (field.getType.getName == "int") 0
        else if (field.getType.getName == "long") 0
        else if (field.getType.getName == "double") 0.0
        else if (field.getType.getName == "boolean") false
      } else {

        // Ensure that values are not HTML escaped
        if (field.getType.getName == "java.lang.String") StringEscapeUtils.unescapeHtml4(field.get(c).toString)
        else field.get(c)
      }
    }
  }
}