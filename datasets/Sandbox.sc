import com.wisecube.orpheus.data.datasets.{ClinicalTrials, MeSH}
import org.apache.spark.sql.SparkSession

import java.nio.file.{Files, Paths}

val spark: SparkSession = SparkSession
  .builder()
  .appName("spark-test")
  .master("local[*]")
  .getOrCreate()


val filepath: String = MeSH.download()
val meshDF = MeSH.load(filepath)
val meshPath = Paths.get("data/mesh_table.parquet")

if (!Files.isDirectory(meshPath)) {
  meshDF.write.parquet(meshPath.toFile.getAbsolutePath)
}

val zipfilePath = ClinicalTrials.download()
val directory = ClinicalTrials.unzip(zipfilePath)

ClinicalTrials.getFiles(directory).foreach(println)
val aactPath = Paths.get("data/aact")

ClinicalTrials.getFiles(directory).foreach {
  filename =>
    val tablename = filename.replace(".txt", "")
    val df = ClinicalTrials.load(filename, directory)
    val path = Paths.get(aactPath.toString, tablename + ".parquet")
    if (!path.toFile.isDirectory) {
      df.write.parquet(path.toFile.getAbsolutePath)
    }
}

meshDF.createOrReplaceTempView("mesh")
spark.read.parquet(
  Paths.get(aactPath.toString, "browse_conditions" + ".parquet")
    .toFile.getAbsolutePath
)

spark.table("mesh").show()