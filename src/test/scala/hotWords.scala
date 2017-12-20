// This is the code of map reduce function, which is perfomed on zeppelin HPC

//val business_data = sqlContext.read.json("file:/home/adj329/home/videoTags")

val business_data = sqlContext.read.json("file:/home/adj329/stags.txt")

business_data.createOrReplaceTempView("tagdata")

val categoryList_Q = "SELECT DISTINCT category FROM tagdata";

val categoryList_DF = sqlContext.sql(categoryList_Q)

val categoryList = categoryList_DF.select("category").rdd.map(r => r(0)).collect()


val r2 = business_data.withColumn("tags", explode(when(col("tags").isNotNull, col("tags"))))

//r2.show(22000)


val textfile = r2.groupBy("category").agg(concat_ws(",", collect_list("tags")) as "tags")


for(cat <- 0 to 30){

  val s = textfile.filter(col("category").like(categoryList(cat).toString)).select("tags")
  val p = s.first().getString(0)
  //print(p)


  val stringRdd = sc.parallelize(List(p))

  val counts = stringRdd.flatMap(line => line.split(","))
    .map(word => (word, 1))
    .reduceByKey(+)
    .map(item => item.swap) // interchanges position of entries in each tuple
    .sortByKey(false, 1)

  counts.toDF().show()



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