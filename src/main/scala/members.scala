import play.api.libs.json._
import scala.collection.mutable.Buffer
import java.util.Calendar



object client_essentials{
    val regex = "(?<=[a-zA-Z]+)(\\.)(?=[a-zA-Z]+)".r
    val numPattern = "(\\d{8})".r
    
    val client_log_mapper= Map("start"->Array("game","__tm", "seq","logid","start_adid","info_uid","info_did", "info_os","info_md", "info_ctver", "info_osver", "info_store","info_oauth","info_ulv","info_appid", "info_country", "time", "timezone"),
        
        "end"->Array("game","__tm","seq","logid","info_uid","info_did","info_ctver", "info_os", "info_osver","info_md", "info_store","info_oauth", "info_ulv","info_appid", "info_country", "time", "timezone"), 
        
        "install"-> Array("game","__tm","seq","logid","info_did", "info_os","info_ctver", "info_osver", "info_store", "info_appid", "info_country", "info_md","time", "timezone","install_adid"),
                           
        "custom"->Array("game","__tm","seq","logid", "info_uid","info_did","info_md", "info_os", "info_osver", "info_store", "info_appid", "user_s","user_n","info_country","custom_nm","custom_category", "info_ulv", "time", "timezone","info_ctver"),
                           
        "purchase"->Array("game","__tm","seq","logid", "info_uid","info_did", "info_os", "info_osver", "info_store", "info_appid", "info_country","info_md","info_oauth","info_ulv","info_ctver","purchase_campaign", "purchase_amount","purchase_currency","purchase_goodscd","purchase_goodsnm","time", "timezone"), 
        
        "postback"->Array("game","__tm","logid","postback_ch","postback_id", "postback_msg_app_group_code", "postback_msg_app_service_code", "postback_msg_device_time_zone_offset", "postback_msg_failure", "postback_msg_push_id", "postback_msg_reason", "postback_msg_success", "postback_partner","time", "timezone"),
        
        "ab"->Array("game","__tm","seq","logid", "info_uid","info_did", "info_md","info_os", "info_osver", "info_store", "info_appid", "info_country","info_ctver","info_oauth","ab_tid","ab_nm", "info_ulv", "time", "timezone","__ts")
        ).withDefaultValue(Array(" "))
        
     val client_logid_xlat = Map("1001"->"install","1003"->"start","1004"->"end","1005"->"purchase","1006"->"custom","1007"->"ab","2000"->"postback","2001"->"postback","2002"->"postback").withDefaultValue("NotFound") 
   
     val client_custom_xlat = Map("shop"->"shop","tutorial"->"tutorial","MAP_Funnel_FTUE"->"ftue","battle_result"->"battle","MAP_Funnel_Story"->"story").withDefaultValue("custom")
  
     
   val client_log_format_map=Map("custom" -> Map("info.uid" -> "char","info.ulv"->"int"), "start"->Map("start.adid"->"char","info.ulv"->"int"),
                              "install"-> Map("Install.adid"->"char"),"end"->Map("info.ulv"->"int") , "postback"->Map("postback.msg.app.group.code"->"char",
                                  "postback.msg.app.service.code"->"char","postback.msg.device.time.zone.offset"->"char","postback.msg.failure"->"char",
                                  "postback.msg.push.id"->"char","postback.msg.reason"->"char","postback.msg.success"->"char","postback.msg.completed"->"char"), 
                                  "purchase"->Map("purchase.campaign"->"int","info.uid"->"char","info.did"->"char","info.os"->"char","info.osver"->"char"))
     
   def getNewKey(oldKey: String, newKey: String): String = {
    if (oldKey.nonEmpty) oldKey + "." + newKey else newKey
  }
    
     def getUniqueTimeStamp():String={
       ((Calendar.getInstance()).get(Calendar.HOUR)).toString + "_" + ((Calendar.getInstance()).get(Calendar.MINUTE)).toString + "_" + ((Calendar.getInstance()).get(Calendar.SECOND)).toString + "_" + ((Calendar.getInstance()).get(Calendar.MILLISECOND)).toString
     }

  def flatten(js: JsValue, prefix: String = ""): JsObject = {
    if (!js.isInstanceOf[JsObject]) return Json.obj(prefix -> js)
    js.as[JsObject].fields.foldLeft(Json.obj()) {
      case (o, (k, value)) => {
        o.deepMerge(value match {
          case x: JsArray => x.as[Seq[JsValue]].zipWithIndex.foldLeft(o) {
            case (o, (n, i: Int)) => o.deepMerge(
              flatten(n.as[JsValue], getNewKey(prefix, k) + s"[$i]")
            )
          }
          case x: JsObject => flatten(x, getNewKey(prefix, k))
          case x => Json.obj(getNewKey(prefix, k) -> x.as[JsValue])
        })
      }
    }
  }  
}