import java.time.format.DateTimeFormatter
import org.json4s._
import org.json4s.jackson.JsonMethods._
import java.io.FileNotFoundException
import scala.collection.mutable.Buffer
import scala.collection.parallel.mutable.ParArray
import java.time.LocalDate
import scala.io.Source
import client_essentials._
import com.typesafe.config.ConfigFactory
import scala.collection.mutable.ListBuffer
import java.io._
//import play.api.libs.json._
object main {
  val informat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  val outFormat = DateTimeFormatter.ofPattern("yyyyMMdd")
  def main (args: Array[String]) {
        if(args.length != 4){
        println("dude, i need just four parameter")
        System.exit(0)
        }
        val startDt = LocalDate.parse(args(0), informat).format(outFormat)
        val endDt = LocalDate.parse(args(1), informat).format(outFormat)
        var listoffiles = new ListBuffer[String]()
        val read_client_obj = new s3client(args(2))
        var workngdt = startDt

        while (workngdt <= endDt){
          val fullClientList: Buffer[String] = read_client_obj.getallkeys(workngdt, workngdt)
          start_reading_client(fullClientList,read_client_obj,workngdt, args(3))
          var tempDt = LocalDate.parse(workngdt, outFormat)
          var temp1 = tempDt.plusDays(1)
          workngdt = temp1.format(outFormat)
        }
        
        try{
           workngdt = (LocalDate.parse(startDt, outFormat).plusDays(-1)).format(outFormat)
           val oldFile = ConfigFactory.load("clientconfig").getString("client.filerepo") + "_" + workngdt+
                         "_" +args(3) + ".txt"
           new File(oldFile).delete()
           }
        catch{
          case ex: FileNotFoundException =>{
                   println("The old File was not found ")
                   }
             }
            
  }
  
    def start_reading_client(fullClientList: Buffer[String], read_client_obj:s3client,startDt:String, game:String) = {
        var seqNums = 1
        val newClientList = (fullClientList).par
        try {
            var fileRepo = ConfigFactory.load("clientconfig").getString("client.filerepo") + "_" + startDt + "_" + game + ".txt"  
            var actualoffiles = get_pre_read_file_data(fileRepo)

            for (files <- newClientList){
                if (!actualoffiles.contains(files)){
                  var fileName = (files.split((System.getProperty("file.separator")).toString).reverse.head).split('.').mkString("_")
                  var fileData = read_client_obj.readFromS3(files)
                  val newClientObj = new clientData(fileData,fileName, game) 
                  newClientObj.parseNaccumulate()
                  newClientObj.writeintoS3(files)
                  actualoffiles += files
                  }
             }
             val writer = new BufferedWriter(new FileWriter(fileRepo)) 
             for (x<-actualoffiles){
                writer.write(x + "\n")
              }
             writer.close()
             }
        catch  {
             case e : Exception => println("I am   in start reading");e.printStackTrace;
                }
       
             println("The file processing is done") 
            }
    
  def get_pre_read_file_data(fileName: String):ListBuffer[String] = {
     try {
         val listOfLines = Source.fromFile(fileName).getLines.toList
          listOfLines.to[ListBuffer]
          }
     catch{
          case ex: FileNotFoundException =>{
                   println("File not found Exception")
                   new ListBuffer[String]()
          }
          case ex: IOException => {
                   println("Input /Output Exception")
                   new ListBuffer[String]()
          }
          
          }
}
}