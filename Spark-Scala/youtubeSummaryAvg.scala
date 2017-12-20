import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.SQLContext


object youtubeSummaryAvg {
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
    val finalDF =
      """SELECT Video.videoId, Video.category,
        | Statistics.viewCount, Statistics.likeCount, Statistics.dislikeCount, Statistics.favoriteCount
        | FROM Video
        | INNER JOIN Statistics
        | ON Video.videoId = Statistics.videoId
      """.stripMargin


    // Executing the above Query
    val VideoStats_DF = spark.sql(finalDF)
    VideoStats_DF.show(20)
    VideoStats_DF.createOrReplaceTempView("CategoryData")


    // Part 1 -- Getting the top videos in each and every category on the basis of view counts
    val trendStats =
      """ SELECT videoId, category, viewCount, likeCount, favoriteCount, dislikeCount
        |  FROM ( SELECT viewCount, videoId, category, likeCount, favoriteCount, dislikeCount, row_number()
        |         over (PARTITION BY category ORDER BY viewCount DESC) as r
        |         FROM CategoryData as T) as T
        |  WHERE T.r<=100
      """.stripMargin

    val trendStats_DF = spark.sql(trendStats)

    trendStats_DF.createOrReplaceTempView("trendStats")

    val trendSummary =
      """ SELECT category, SUM(viewCount), SUM(likeCount), SUM(dislikeCount), SUM(favoriteCount)
        | FROM trendStats
        | GROUP BY category
      """.stripMargin

    val trendSummary_DF = spark.sql(trendSummary)

    trendSummary_DF.coalesce(1).write.format("com.databricks.spark.csv")
      .option("header", "true")
      .save("./Data/trendStats.csv")

  }
}
