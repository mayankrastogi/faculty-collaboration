package com.mayankrastogi.cs441.hw2.mapreduce

import com.mayankrastogi.cs441.hw2.utils.Settings
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.{IntWritable, Text}
import org.apache.hadoop.mapreduce.Job
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.mapreduce.lib.output.{FileOutputFormat, TextOutputFormat}
import org.apache.mahout.text.wikipedia.XmlInputFormat

object FacultyCollaborationDriver extends LazyLogging {
  private val settings = new Settings(ConfigFactory.load())

  def main(args: Array[String]): Unit = {
    runJob(args(0), args(1))
  }

  def runJob(inputDir: String, outputDir: String): Unit = {
    logger.trace(s"runJob(inputPath: $inputDir, outputPath: $outputDir)")

    logger.debug("Configuring MapReduce Job...")

    val conf = new Configuration()
    conf.set(XmlInputFormat.START_TAG_KEY, "<article")
    conf.set(XmlInputFormat.END_TAG_KEY, "</article>")

    val job = Job.getInstance(conf, settings.jobName)

    job.setJarByClass(this.getClass)
    job.setOutputKeyClass(classOf[Text])
    job.setOutputValueClass(classOf[IntWritable])
    job.setMapperClass(classOf[FacultyCollaborationMapper])
    job.setCombinerClass(classOf[FacultyCollaborationReducer])
    job.setReducerClass(classOf[FacultyCollaborationReducer])
    job.setInputFormatClass(classOf[XmlInputFormat])
    job.setOutputFormatClass(classOf[TextOutputFormat[Text, IntWritable]])

    FileInputFormat.setInputPaths(job, new Path(inputDir))

    val outputPath = new Path(outputDir)
    FileOutputFormat.setOutputPath(job, outputPath)

    // Clean output directory
    logger.trace("Removing any existing output files")
    val outputDirectoryDeleted = outputPath.getFileSystem(conf).delete(outputPath, true)
    if (outputDirectoryDeleted) {
      logger.debug(s"Deleted existing output files")
    }
    else {
      logger.debug("Output path already clean")
    }

    logger.info("Submitting job and waiting for completion...")
    job.waitForCompletion(true)
    logger.info("Job completed.")
  }

}