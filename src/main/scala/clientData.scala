

import java.io.{BufferedWriter, FileWriter}
import scala.math._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import client_essentials._
import scala.util.parsing.json.JSONObject
//import scala.annotation.switch
import scala.collection.parallel.mutable.ParMap
import scala.util.matching.Regex
import com.amazonaws.services.s3.AmazonS3Client
import com.typesafe.config.ConfigFactory
import java.io._
import java.util.zip._
import java.time.LocalDate
import java.time.format.DateTimeFormatter
//import com.amazonaws.services.s3.AmazonS3ClientBuilder
import jsontoMap._
import org.json4s.jackson.Serialization
import com.amazonaws.auth.BasicAWSCredentials
//import play.api.libs.json._

class clientData(clientFile:String, fileName: String, game:String ) {
  val accessKey=ConfigFactory.load("s3res").getString("s3.key.access")
  val secretKey=ConfigFactory.load("s3res").getString("s3.key.secret")
  val credentials = new BasicAWSCredentials (accessKey, secretKey) 
  val s3 = new AmazonS3Client (credentials)
  //val s3 = AmazonS3ClientBuilder.defaultClient()
  val bucketName=ConfigFactory.load("s3res").getString("s3.repo.name")
  val prefix=ConfigFactory.load("s3res").getString("s3.repo.prefix")
 
  val clientData = scala.io.Source.fromString(clientFile).getLines.toList
  //var splitName = fileName.replace('/','_')
  var customLogInd = false
  val informat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  val outFormat = DateTimeFormatter.ofPattern("yyyyMMdd")
  val clientRepo = collection.parallel.mutable.ParMap[String,scala.collection.mutable.ArrayBuffer[String]]()
  val customRepo = collection.parallel.mutable.ParMap[String,scala.collection.mutable.ArrayBuffer[String]]()
  //*************Main Routine************************
      def parseNaccumulate() = {
          try {
              for (items <- clientData){
                  //println("Now reading this " + items)
                  var jsonp = parse(items)
                  //val clientString = jsonStrToMap(items)
                  //println("The log type is " + (jsonp \ "logid").as[Int].toString)
                  var logType =  client_logid_xlat(compact(render(jsonp \\ "logid")))
                  var logCategory = "ignore"
                  if (logType == "custom") {
                      val cstmstr = play.api.libs.json.Json.parse(items)
                      var temphold = flatten(cstmstr).toString 
                      var parsetemp = parse(temphold)
                      val rawcat = compact(render(parsetemp \\ "custom.category")) 
                      logCategory = client_custom_xlat(rawcat.replace("\"","")) 
                      customLogInd = true
                  }
                  check_n_correct_missing_fields(items,logType,logCategory)
               } 
               clientRepo
               }
          catch 
               {
                case e : Exception => println("Issue encountered while reading LogType" + fileName);e.printStackTrace;
               }
           }
  
  //*************End of Routine**********************
  
      def changeToString(logType:String,logData:scala.collection.mutable.Map[String,Any]) = {
           val recordList = client_log_mapper(logType)
           var dataString=""
           var ctr = 0
           for (columns <- recordList){
               if (logData.contains(columns)){
                   if (ctr == 0) {
                      dataString+=logData(columns)
                        }
                   else {
                        dataString+="|" + logData(columns)
                        }
               }
               else
               {
                  if (ctr > 0)  {
                        dataString+="|" 
                        }
               }
               ctr += 1
              }
           dataString
          }
  
      def check_n_correct_missing_fields(formatString:String,logType:String,logCategory:String)={
          //println("I am in check_n_correct_missing_fields")
          var formattedString=scala.collection.mutable.Map[String,Any]()
          var clientString = jsonStrToMap(formatString)
          logType match {
              case "custom" => {
                               val temp1 = clientString.filterKeys(_ != "user_s").filterKeys(_ != "user_n")
                               val temp2 = change_map_to_string(collection.mutable.Map(temp1.toSeq: _*)) 
                               formattedString = change_string_to_map(temp2)
                               if (clientString.contains("user_s"))  
                                  {formattedString("user_s") = 
                                     JSONObject(clientString("user_s").asInstanceOf[Map[String, Any]]).toString()
                                     } 
                               else 
                                 formattedString("user_s") = ""
                               if (clientString.contains("user_n"))  
                                  {formattedString("user_n") = 
                                    JSONObject(clientString("user_n").asInstanceOf[Map[String, Any]]).toString()
                                   } else 
                                     formattedString("user_n") = ""
                               }
              case _ =>       {
                               formattedString = change_string_to_map(formatString)
                              }
              }
          if (logCategory == "ignore"){
              if (clientRepo.contains(logType)){
                   clientRepo(logType) += changeToString(logType, formattedString)
                 }
              else
                 {
                   clientRepo.put(logType,scala.collection.mutable.ArrayBuffer(changeToString(logType, formattedString)))
                 }
              }
          else 
              {
                if (customRepo.contains(logCategory)){
                   customRepo(logCategory) += changeToString(logType, formattedString)
                 }
              else
                 {
                   customRepo.put(logCategory,scala.collection.mutable.ArrayBuffer(changeToString(logType, formattedString)))
                 }
              }
          } 
      

  
      def change_map_to_string(amap:scala.collection.mutable.Map[String,Any]):String={
          implicit val formats = org.json4s.DefaultFormats
          Serialization.write(amap)
          }
      
      def change_string_to_map(astr:String)={
          //println("Just before parsing the string")
          val jsonp = play.api.libs.json.Json.parse(astr)
          val temp3 = flatten(jsonp).toString
          //println("Just after parsing the string")
          val formatString = regex.replaceAllIn(temp3, "_")
          jsonStrToMap(formatString)
      }
  
      def writeintoS3(aFile:String){
          val match1 = numPattern.findFirstIn(aFile)
          val sdate = match1.getOrElse("")
          val adate = LocalDate.parse(sdate, outFormat).format(informat)
          read_repo_n_write_into_s3(clientRepo, adate,false)
          if (customLogInd) 
             {
             //println("now printing custom folders")
             read_repo_n_write_into_s3(customRepo, adate,true)
             }
          }
      
      def read_repo_n_write_into_s3(arepo:collection.parallel.mutable.ParMap[String,scala.collection.mutable.ArrayBuffer[String]],adate:String, customInd:Boolean) = {
          arepo.foreach{keyVal => 
                
                val areallylongline = keyVal._2.mkString(System.lineSeparator)
                var tempFile = ConfigFactory.load("clientconfig").getString("client.tempLoc") + 
                               (System.getProperty("file.separator")).toString+keyVal._1  
                if (customInd){
                  tempFile = ConfigFactory.load("clientconfig").getString("client.tempLoc") + 
                             (System.getProperty("file.separator")).toString  +"data_custom" + 
                              (System.getProperty("file.separator")).toString+ keyVal._1  
                  }
                tempFile += (System.getProperty("file.separator")).toString + "client_log_" +  fileName + "_" + keyVal._1 + "_" + adate + "_"  + ".txt.gz"
                var fos = new FileOutputStream(tempFile)
                var gzos = new GZIPOutputStream( fos )
                var w = new PrintWriter(gzos)
                w.write(areallylongline)
                w.close()
                gzos.close()
                fos.close()
                uploadntos3(keyVal._1, tempFile,adate,customInd)
                }
          
          }
      
      def uploadntos3(logtype:String, atempFile:String,adate:String,customInd:Boolean)={
          val rawFileName = fileName + "_"  + ".txt.gz"
          val basefolder = this.prefix + game + (System.getProperty("file.separator")).toString
          var keyName = basefolder + logtype + 
                         (System.getProperty("file.separator")).toString + "dt=" + adate + 
                         (System.getProperty("file.separator")).toString + rawFileName
           if (customInd) {
              keyName = basefolder + "data_custom" + 
                         (System.getProperty("file.separator")).toString + logtype + 
                         (System.getProperty("file.separator")).toString + "dt=" + adate + 
                         (System.getProperty("file.separator")).toString + rawFileName
              }
          s3.putObject(bucketName, keyName, new File(atempFile));
          new File(atempFile).delete()  //After a successfull upload delete the file
      }
  
  }