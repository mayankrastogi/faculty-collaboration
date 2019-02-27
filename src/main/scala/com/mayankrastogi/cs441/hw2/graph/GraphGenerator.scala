package com.mayankrastogi.cs441.hw2.graph

import java.io.{File, FileNotFoundException, PrintWriter}
import java.nio.file.AccessDeniedException

import com.mayankrastogi.cs441.hw2.utils.Settings
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

import scala.io.Source

object GraphGenerator extends LazyLogging {

  val settings = new Settings(ConfigFactory.load())

  def main(args: Array[String]): Unit = {
    logger.trace("GraphGenerator main called with arguments: " + args.mkString)

    if (args.isEmpty) {
      logger.error("Error: Please specify path to the output file of the map reduce job")
      System.exit(-1)
    }

    generate(args.head)
  }

  def generate(mapReduceOutputFile: String): Unit = {
    logger.trace(s"generate($mapReduceOutputFile)")

    try {
      logger.info("Looking for map reduce output file: " + mapReduceOutputFile)
      val mapReduceOutputSummary = parseMapReduceOutput(Source.fromResource(mapReduceOutputFile).mkString)

      logger.info("Generating dot file...")
      writeDotFile(
        mapReduceOutputSummary,
        settings.graphOutputFilePath,
        settings.graphName,
        settings.graphLabelText,
        settings.graphColorSchemeName,
        settings.graphColorSchemeNumberOfColors,
        settings.graphLabelFontSize,
        settings.graphDimensions,
        settings.graphOverlap,
        settings.graphSeparation,
        settings.graphSplines
      )
    }
    catch {
      case e: FileNotFoundException =>
        logger.error("Could not find file at " + mapReduceOutputFile, e)
      case e: AccessDeniedException =>
        logger.error("Permission denied for accessing file at " + mapReduceOutputFile, e)
      case e: SecurityException =>
        logger.error("Permission denied for accessing file at " + mapReduceOutputFile, e)
    }
  }

  def parseMapReduceOutput(outputFromMapReduce: String): MapReduceOutputSummary = {
    logger.trace(s"parseMapReduceOutput($outputFromMapReduce)")

    outputFromMapReduce.split("\n").foldLeft(new MapReduceOutputSummary)((summary, line) => {
      val keyAndValue = line.trim.split("\t+")
      val nodeOrEdge = keyAndValue.head.split(settings.collaboratorsEdgeIndicator)
      val publications = keyAndValue.last.toInt

      nodeOrEdge.size match {
        case 1 =>
          val facultyList = summary.faculties ++ List(new Faculty(nodeOrEdge.head, publications))
          val maxPublicationsByFaculty = Math.max(summary.maxPublicationsByFaculty, publications)
          new MapReduceOutputSummary(facultyList, summary.collaborations, maxPublicationsByFaculty, summary.maxCollaborationsByFaculty)

        case 2 =>
          val collaborationList = summary.collaborations ++ List(new Collaboration(nodeOrEdge.head, nodeOrEdge.last, publications))
          val maxCollaborationsByFaculties = Math.max(summary.maxCollaborationsByFaculty, publications)
          new MapReduceOutputSummary(summary.faculties, collaborationList, summary.maxPublicationsByFaculty, maxCollaborationsByFaculties)

        case _ => summary
      }
    })
  }

  def writeDotFile(mapReduceOutputSummary: MapReduceOutputSummary,
                   outputPath: String,
                   graphName: String,
                   graphLabel: String,
                   colorScheme: String,
                   colorsInColorScheme: Int,
                   labelFontSize: Double,
                   dimensions: Int,
                   overlap: String,
                   nodeSeparation: Double,
                   splines: Boolean): Unit = {
    logger.trace(s"writeDotFile(mapReduceOutputSummary: $mapReduceOutputSummary, outputPath: $outputPath, graphName: $graphName, graphLabel: $graphLabel, colorScheme: $colorScheme, colorsInColorScheme: $colorsInColorScheme, labelFontSize: $labelFontSize, dimensions: $dimensions, overlap: $overlap, nodeSeparation: $nodeSeparation, splines: $splines)")

    val nodeSizeScalingMultiplier = 1.0 / Math.log1p(mapReduceOutputSummary.maxPublicationsByFaculty)

    val nodeOutput = mapReduceOutputSummary.faculties.foldLeft("")((builtOutput, faculty) => {

      val logOfPublications = Math.log1p(faculty.publications)
      val nodeSize = logOfPublications * nodeSizeScalingMultiplier
      val color = Math.ceil(nodeSize * colorsInColorScheme).toInt

      s"""$builtOutput
         |    "${faculty.name}" [ label="${faculty.name}\\n(${faculty.publications})", width=$nodeSize, color=$color ];
         |""".stripMargin
    })

    val edgeOutput = mapReduceOutputSummary.collaborations.foldLeft("")((builtOutput, collaboration) => {
      val edgeWeight = Math.max(4.0 * collaboration.publications.toDouble / mapReduceOutputSummary.maxCollaborationsByFaculty.toDouble, 1.0)

      s"""$builtOutput
         |    "${collaboration.faculty1}" -- "${collaboration.faculty2}" [ label="${collaboration.publications}", weight=${collaboration.publications}, penwidth=$edgeWeight ];
         |""".stripMargin
    })

    val dot =
      s"""graph $graphName {
         |    graph [ label="$graphLabel", fontsize=$labelFontSize, pad=0.5, overlap=$overlap, sep=$nodeSeparation, dimen=$dimensions, splines=$splines ];
         |    node [ shape=circle, fixedsize=true, style=filled, colorscheme=$colorScheme ];
         |    $nodeOutput
         |    $edgeOutput
         |}
         |""".stripMargin

    logger.debug("Generated dot file contents:\n" + dot)

    val writer = new PrintWriter(new File(outputPath))
    writer.write(dot)
    writer.close()

    logger.info("GraphViz dot file generated at: " + outputPath)
  }
}
