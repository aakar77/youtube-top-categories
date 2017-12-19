import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.SQLContext


object youtubeCSV {
  def main(args: Array[String]): Unit = {

    // SECTION - 0 SPARK CONFIG

    // Code for setting the Spark Configuration with MemSQL
    val conf = new SparkConf()
      .setAppName("MemSQL Spark Connector Example")
      .set("spark.memsql.host", "localhost")
      .set("spark.memsql.port", "3306")
      .set("spark.memsql.defaultDatabase", "youtubeDB")

    conf.setAppName("YoutubeApi")
    conf.setMaster("local[2]")

    // Creating the Spark Session
    val spark = SparkSession.builder().config(conf).getOrCreate()

    //-- SECTION 1 - Video Table CONFIG

    // Creating a Video Table RDF and connection
    val videoRDF = spark
      .read
      .format("com.memsql.spark.connector")
      .options(Map("path" -> ("youtubeDB.Video")))
      .load()


    // After loading the data, creating it as a temporary view as Video
    videoRDF.createOrReplaceTempView("Video");

    //-- SECTION 2 - Video Table CONFIG

    val statisticsRDF = spark
      .read
      .format("com.memsql.spark.connector")
      .options(Map("path" -> ("youtubeDB.Statistics")))
      .load()

    statisticsRDF.createOrReplaceTempView("Statistics")
    // SECTION 3 Query for General data basis of TOP 10 View Count (Trending)

    // Joining the Video and Statistics Table for Getting the Data
    val videoQuery =
      """SELECT * FROM Video
      """.stripMargin

    // Executing the above Query
    val videoDF = spark.sql(videoQuery)

    videoDF.coalesce(1).write.format("com.databricks.spark.csv")
      .option("header", "true")
      .save("./Data/videoData.csv")

    val statsQuery = """ SELECT * FROM Statistics"""
    val statsDF = spark.sql(statsQuery)


    statsDF.coalesce(1).write.format("com.databricks.spark.csv")
      .option("header", "true")
      .save("./Data/statsData.csv")
  }
}
