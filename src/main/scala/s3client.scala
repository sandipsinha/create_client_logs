import java.time.temporal.ChronoUnit
import scala.concurrent.{Future}
import scala.io.Source
import com.amazonaws.services.s3.model.ListObjectsRequest
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3 ._
import com.typesafe.config.ConfigFactory
import com.amazonaws.services.s3.AmazonS3Client
import scala.collection.JavaConverters._
import com.amazonaws ._ 
import java.text.SimpleDateFormat
import java.util.Calendar
import java.time.format.DateTimeFormatter
import com.amazonaws.services.s3.model.ObjectListing
import java.time.LocalDate
import java.util.ArrayList
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.util.IOUtils;
import scala.concurrent.ExecutionContext.Implicits.global

import com.amazonaws.auth.BasicAWSCredentials
import java.net.URI

class s3client(sourcePrefix:String) {
     val informat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
     val outFormat = DateTimeFormatter.ofPattern("yyyyMMdd")
     //println("now uploading into s3 " + filePath)
     val accessKey=ConfigFactory.load("s3res").getString("s3.key.access")
     val secretKey=ConfigFactory.load("s3res").getString("s3.key.secret")
     val credentials = new BasicAWSCredentials (accessKey, secretKey) 
    
     var s3Client = new AmazonS3Client(credentials)
     val bucketName=ConfigFactory.load("s3res").getString("s3.bucket.name")
     val baseprefix=ConfigFactory.load("s3res").getString("s3.bucket.prefix")
     var objectListing: ObjectListing = null
     var listObjectsRequest = new ListObjectsRequest();
     listObjectsRequest.setBucketName(bucketName)
     var files = new ArrayList[String]  
     
     
     
     def getallkeys(startDt:String,endDt:String) = {
       
       var workingDt = startDt
       var workingDate=LocalDate.parse(workingDt, outFormat)
       while (workingDt <= endDt){
         
         val actualprefix = baseprefix + sourcePrefix + "/plogdate=" + workingDt 
         //println("the folder name of the path is " + actualprefix)
         listObjectsRequest.setPrefix(actualprefix)
         //--------
         try{
          //Adding s3:// to the paths and adding to a list
          do {
            objectListing = s3Client.listObjects(listObjectsRequest);
            for (objectSummary <- objectListing.getObjectSummaries().asScala) {
              var fileName = objectSummary.getKey()
              //println("the file name of the key is " + fileName + " And the length is " + fileName.length)
              if (fileName.length > 5 ){
                 files.add(fileName)
              } 
             }
             listObjectsRequest.setMarker(objectListing.getNextMarker());
           } while (objectListing.isTruncated());
         }
           catch  {
                case e:AmazonServiceException if e.getStatusCode == 404 => println("Error connecting to S3")
                case e: Throwable => {println("Some other S3 connection issue");}
            }
        //println("the length of the file is " + files.size())
        //Removing Base Directory Name
        //files.remove(0)
        
        //Creating a Scala List for same
        
        workingDate =  workingDate.plus(1, ChronoUnit.DAYS)
        workingDt = workingDate.format(outFormat)
       }
       
       files.asScala
     }
     
     def  readFromS3(key :String):String = {
        try {
            val s3Object: S3Object  = s3Client.getObject(new GetObjectRequest(bucketName, key))
            val fileContent = s3Object.getObjectContent()
            IOUtils.toString(fileContent) 
            }
        catch {
            case e : Exception => println("It is a general exception in readfroms3");e.printStackTrace; " "
            }
     }
}