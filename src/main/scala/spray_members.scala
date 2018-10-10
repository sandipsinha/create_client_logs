import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization

object jsontoMap {
  
  def jsonStrToMap(jsonStr: String): scala.collection.mutable.Map[String, Any] = {
      implicit val formats = org.json4s.DefaultFormats
      val temp = parse(jsonStr).extract[Map[String, Any]]
      collection.mutable.Map(temp.toSeq: _*)
      }
  
  
}