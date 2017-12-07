name := "youtubeApiTest"

//version := "0.1"
//
//
//scalaVersion := "2.11.12"
//
//// https://mvnrepository.com/artifact/org.apache.spark/spark-core
//libraryDependencies += "org.apache.spark" %% "spark-core" % "2.2.0"
//
//// https://mvnrepository.com/artifact/org.apache.spark/spark-sql
//libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.2.0"
//
//// https://mvnrepository.com/artifact/org.apache.spark/spark-streaming
//libraryDependencies += "org.apache.spark" %% "spark-streaming" % "2.2.0" % "provided"
//
//// Memsql connector dependency
//libraryDependencies  += "com.memsql" %% "memsql-connector" % "2.0.2"


version := "1.0"

scalaVersion := "2.11.1"
libraryDependencies += "org.apache.spark" % "spark-core_2.11" % "2.2.0"
libraryDependencies += "org.apache.spark" % "spark-sql_2.11" % "2.2.0"
libraryDependencies += "org.apache.spark" % "spark-sql-kafka-0-10_2.11" % "2.2.0"
libraryDependencies += "org.apache.spark" % "spark-streaming_2.11" % "2.2.0"
libraryDependencies += "com.memsql" % "memsql-connector_2.11" % "2.0.2"
libraryDependencies += "org.apache.spark" % "spark-streaming-kafka-0-8_2.11" % "2.0.0-preview"
libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.6"
