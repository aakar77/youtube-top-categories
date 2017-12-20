
// This is the code of map reduce function, which is perfomed on zeppelin HPC

//val business_data = sqlContext.read.json("file:/home/adj329/home/videoTags")

// Reading the data from videoTags file
val business_data = sqlContext.read.json("file:/home/adj329/videoTags")

business_data.createOrReplaceTempView("tagdata")

// getting all video categories
val categoryList_Q = "SELECT DISTINCT category FROM tagdata";

// executing the query
val categoryList_DF = sqlContext.sql(categoryList_Q)

//
val categoryList = categoryList_DF.select("category").rdd.map(r => r(0)).collect()

// exploding the tags for a given category
val r2 = business_data.withColumn("tags", explode(when(col("tags").isNotNull, col("tags"))))

//r2.show(22000)

// Groupin all the tags into one field for a given category
val textfile = r2.groupBy("category").agg(concat_ws(",", collect_list("tags")) as "tags")


// Iterating over all 31 categories and doing map reduce tasks for each category
for(cat <- 0 to 30){

  // Getting tag words for the given category pointed by categoryList(cat) and convert into string, seperated by ','
  val s = textfile.filter(col("category").like(categoryList(cat).toString)).select("tags")
  val p = s.first().getString(0)
  
  print("For Category : "+categoryList(cat))
  print("\n")

  // converting the string into RDD
  val stringRdd = sc.parallelize(List(p))

 // Map - reduce word count for tags for a category, data seperated by ','' 
  val counts = stringRdd.flatMap(line => line.split(","))
    .map(word => (word, 1))
    .reduceByKey(_+_)
    .map(item => item.swap) // interchanges position of entries in each tuple
    .sortByKey(false, 1)

  // converting the data into Data frame
  val categoryCount = counts.toDF()
  
  
  categoryCount.createOrReplaceTempView("tagdata")
  
  // selecting top 25 in each category data frame
  val top25_Q = "SELECT _1 as Count, _2 as TopTags FROM tagdata LIMIT 25"
  
  val top25_DF = sqlContext.sql(top25_Q)
  
  // writting into CSV file
  val fileName = "file:/home/adj329/Top25CategoryTags/"+categoryList(cat)+".csv"
  
    top25_DF.coalesce(1).write.format("com.databricks.spark.csv")
      .option("header", "true")
      .save(fileName)
}


/*
for (cat <- 0 to 1){
   //println(categoryList[cat])
   val categoryRDF = r2.filter(col("category").like(categoryList(cat).toString))
   categoryRDF.agg(concat_ws(" ", collect_list("tags")) as "tags").show()
   //println(categoryRDF.count())
    array[0][1]
}
*/
