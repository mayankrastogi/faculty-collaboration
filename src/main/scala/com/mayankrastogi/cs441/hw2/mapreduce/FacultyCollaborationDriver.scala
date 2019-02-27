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

/**
  * Configures the map-reduce job and runs it.
  */
object FacultyCollaborationDriver extends LazyLogging {

  // Load application settings
  private val settings = new Settings(ConfigFactory.load())

  /**
    * Runs the map-reduce job.
    *
    * @param args The first argument specifies the input directory. The second argument specifies the output directory.
    */
  def main(args: Array[String]): Unit = {
    logger.trace(s"main(args: ${args.mkString})")

    // Print error message if input and/or output paths are not provided as arguments
    if(args.length < 2) {
      logger.error("Input and/or output paths not provided.")
      System.exit(-1)
    }

    // Run the map-reduce job
    runJob(args(0), args(1))
  }

  /**
    * Runs the map-reduce job.
    *
    * @param inputDir The location of input files for the map-reduce job.
    * @param outputDir The location of where output of map-reduce job will be saved.
    */
  def runJob(inputDir: String, outputDir: String): Unit = {
    logger.trace(s"runJob(inputPath: $inputDir, outputPath: $outputDir)")

    logger.debug("Configuring MapReduce Job...")

    // Configure the start and end tags to search for in the XML file
    val conf = new Configuration()

    conf.setStrings(MultiTagXmlInputFormat.START_TAG_KEY, settings.xmlInputStartTags: _*)
    conf.setStrings(MultiTagXmlInputFormat.END_TAG_KEY, settings.xmlInputEndTags: _*)

    // Configure the map-reduce job
    val job = Job.getInstance(conf, settings.jobName)

    job.setJarByClass(this.getClass)
    job.setOutputKeyClass(classOf[Text])
    job.setOutputValueClass(classOf[IntWritable])
    job.setMapperClass(classOf[FacultyCollaborationMapper])
    job.setCombinerClass(classOf[FacultyCollaborationReducer])
    job.setReducerClass(classOf[FacultyCollaborationReducer])
    job.setInputFormatClass(classOf[MultiTagXmlInputFormat])
    job.setOutputFormatClass(classOf[TextOutputFormat[Text, IntWritable]])

    // Set input and output paths
    FileInputFormat.setInputPaths(job, new Path(inputDir))

    val outputPath = new Path(outputDir)
    FileOutputFormat.setOutputPath(job, outputPath)

    // Clean output directory if it exists
    logger.trace("Removing any existing output files")
    val outputDirectoryDeleted = outputPath.getFileSystem(conf).delete(outputPath, true)
    if (outputDirectoryDeleted) {
      logger.debug(s"Deleted existing output files")
    }
    else {
      logger.debug("Output path already clean")
    }

    // Run the job and wait for its completion
    logger.info("Submitting job and waiting for completion...")
    job.waitForCompletion(true)
    logger.info("Job completed.")
  }

}
