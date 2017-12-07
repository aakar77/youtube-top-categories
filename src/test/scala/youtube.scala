import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession


object youtube {
  def main(args: Array[String]): Unit = {

    val conf = new SparkConf()
      .setAppName("MemSQL Spark Connector Example")
      .set("spark.memsql.host", "localhost")
      .set("spark.memsql.port", "3306")
      .set("spark.memsql.defaultDatabase", "test")

    conf.setAppName("YoutubeApi")
    conf.setMaster("local[2]")

    val spark = SparkSession.builder().config(conf).getOrCreate()

    val customersFromIllinois = spark
      .read
      .format("com.memsql.spark.connector")
      .options(Map("path" -> ("test.Video")))
      .load()

    // count the number of rows
    println(s"The number of customers from Illinois is ${customersFromIllinois.count()}")

    // print out the DataFrame
    customersFromIllinois.show()

  }
}
