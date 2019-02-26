package com.mayankrastogi.cs441.hw2.graph

import java.io.{File, PrintWriter}

import com.mayankrastogi.cs441.hw2.utils.Settings
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

import scala.io.Source

object GraphGenerator extends LazyLogging {

  val settings = new Settings(ConfigFactory.load())

//  def main(args: Array[String]): Unit = {
//    generate("output-full-only-articles")
//  }

  def generate(mapReduceOutputFile: String): Unit = {
    val nodesAndEdges = parseMapReduceOutput(Source.fromResource(mapReduceOutputFile).mkString)

    writeDotFile(nodesAndEdges._1, nodesAndEdges._2, settings.graphOutputFilePath, settings.graphName)
  }

  def parseMapReduceOutput(outputFromMapReduce: String): (List[Faculty], List[Collaboration]) = {

    outputFromMapReduce.split("\n").foldLeft(List[Faculty]() -> List[Collaboration]())((tuple, line) => {
      val keyAndValue = line.trim.split("\t+")
      val nodeOrEdge = keyAndValue.head.split(settings.collaboratorsEdgeIndicator)

      val facultyList = tuple._1
      val collaborationList = tuple._2

      nodeOrEdge.size match {
        case 1 => facultyList ++ List(new Faculty(nodeOrEdge.head, keyAndValue.last.toInt)) -> collaborationList
        case 2 => facultyList -> (collaborationList ++ List(new Collaboration(nodeOrEdge.head, nodeOrEdge.last, keyAndValue.last.toInt)))
        case _ => facultyList -> collaborationList
      }
    })
  }

  def writeDotFile(facultyList: List[Faculty], collaborationList: List[Collaboration], outputPath: String, graphName: String): Unit = {

    val nodeOutput = facultyList.foldLeft("")((builtOutput, faculty) => {
      s"""$builtOutput
         |    "${faculty.name}" [ label="${faculty.name}\\n(${faculty.publications})", width=${faculty.publications} ];
         |""".stripMargin
    })

    val edgeOutput = collaborationList.foldLeft("")((builtOutput, collaboration) => {
      s"""$builtOutput
         |    "${collaboration.faculty1}" -- "${collaboration.faculty2}" [ label="${collaboration.publications}", weight=${collaboration.publications} ];
         |""".stripMargin
    })

    val dot =
      s"""graph $graphName {
         |    node [ shape=circle, fixedsize=true ];
         |    $nodeOutput
         |    $edgeOutput
         |}
         |""".stripMargin

    logger.info(dot)

    val writer = new PrintWriter(new File(outputPath))
    writer.write(dot)
    writer.close()
  }
}
