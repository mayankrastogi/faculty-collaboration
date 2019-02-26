package com.mayankrastogi.cs441.hw2.utils

import scala.io.Source

object Utils {

  def getFacultyLookupTable(fileName: String): Map[String, String] = {
    val facultyListFile = Source.fromResource(fileName)
    val pattern = """(\b.+\b)\s*=\s*(\b.+\b)""".r("key", "value")

    val lookupTable = facultyListFile.getLines().map(line => {
      val regexMatch = pattern.findAllIn(line)
      regexMatch.group("key") -> regexMatch.group("value")
    }).toMap
    facultyListFile.close()

    lookupTable
  }

  def getDTDFilePath(absolutePath: String, resourcePath: String = ""): String = {
    if(absolutePath != null && absolutePath.nonEmpty)
      absolutePath
    else
      getClass.getClassLoader.getResource(resourcePath).toURI.toString
  }
}
